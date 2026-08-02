package com.wishtrace.app.data

import com.wishtrace.app.domain.ApprovalSession
import com.wishtrace.app.domain.AvailabilityState
import com.wishtrace.app.domain.BillingContact
import com.wishtrace.app.domain.CandidateRationale
import com.wishtrace.app.domain.CandidateRejection
import com.wishtrace.app.domain.CandidateRejectionReason
import com.wishtrace.app.domain.DiscoveryPreparation
import com.wishtrace.app.domain.DiscoveryStage
import com.wishtrace.app.domain.GiftDiscoveryRequest
import com.wishtrace.app.domain.MandateDetails
import com.wishtrace.app.domain.MandateMerchantOutcome
import com.wishtrace.app.domain.MandateStatus
import com.wishtrace.app.domain.MandateVisaConfirmation
import com.wishtrace.app.domain.Money
import com.wishtrace.app.domain.ProductCandidate
import com.wishtrace.app.domain.PurchaseIntentDetails
import com.wishtrace.app.domain.RankedDecision
import com.wishtrace.app.domain.RankingUncertainty
import com.wishtrace.app.domain.SourceMode
import com.wishtrace.app.domain.TransactionState
import com.wishtrace.app.domain.VerifiedResult
import java.time.Instant
import org.json.JSONArray
import org.json.JSONObject

class BackendGiftFlowRepository(
    private val api: WishTraceApiClient,
    private val sessionStore: SessionStore,
) : GiftDiscoveryGateway, PurchaseFlowGateway, MandateGateway {
    override suspend fun prepareCandidates(
        request: GiftDiscoveryRequest,
        onStage: suspend (DiscoveryStage) -> Unit,
    ): DiscoveryPreparation = authenticated { token ->
        onStage(DiscoveryStage.CHECKING_CATALOG)
        val discovery = api.post(
            path = "/v1/discoveries",
            json = JSONObject()
                .put("recipient_id", request.recipientId)
                .put("occasion_id", request.occasionId),
            accessToken = token,
            readTimeoutMillis = 60_000,
        )
        val candidatesJson = discovery.getJSONArray("candidates")
        val merchantId = discovery.requiredString("merchant_id")
        val merchantName = discovery.requiredString("merchant_name")
        val candidates = buildList {
            for (index in 0 until candidatesJson.length()) {
                add(candidatesJson.getJSONObject(index).toCandidate(merchantId, merchantName))
            }
        }
        val eligibleIds = buildList {
            for (index in 0 until candidatesJson.length()) {
                val candidate = candidatesJson.getJSONObject(index)
                if (candidate.getBoolean("eligible")) add(candidate.requiredString("id"))
            }
        }
        if (eligibleIds.isEmpty()) {
            throw WishTraceApiException(
                message = "No live gift currently passes the saved budget and checkout rules.",
                code = "NO_ELIGIBLE_CANDIDATES",
                recoverable = true,
            )
        }

        onStage(DiscoveryStage.APPLYING_BUDGET)
        onStage(DiscoveryStage.CHECKING_FULFILLMENT)
        onStage(DiscoveryStage.PREPARING_RANKING)
        val ranking = api.post(
            path = "/v1/discoveries/${discovery.requiredString("id")}/rank",
            accessToken = token,
            readTimeoutMillis = 60_000,
        )
        val evidenceIds = ranking.getJSONArray("evidence").ids("id").toSet()
        val rejections = buildList {
            for (index in 0 until candidatesJson.length()) {
                val candidate = candidatesJson.getJSONObject(index)
                if (!candidate.isNull("rejection")) {
                    val rejection = candidate.getJSONObject("rejection")
                    add(
                        CandidateRejection(
                            candidateId = candidate.requiredString("id"),
                            reason = rejectionReason(rejection.requiredString("code")),
                            explanation = rejection.requiredString("reason"),
                        ),
                    )
                }
            }
        }
        val decision = ranking.toDecision(rejections).validatedAgainst(
            allCandidateIds = candidates.map { it.id }.toSet(),
            eligibleCandidateIds = eligibleIds.toSet(),
            evidenceIds = evidenceIds,
        )
        DiscoveryPreparation(
            discoveryId = discovery.requiredString("id"),
            candidates = candidates,
            decision = decision,
            eligibleCandidateIds = eligibleIds,
            sourceMode = SourceMode.LIVE,
        )
    }

    override suspend fun createIntent(candidateId: String): PurchaseIntentDetails =
        authenticated { token ->
            api.post(
                path = "/v1/purchase-intents",
                json = JSONObject().put("candidate_id", candidateId),
                accessToken = token,
            ).toPurchaseIntent()
        }

    override suspend fun getIntent(purchaseIntentId: String): PurchaseIntentDetails =
        authenticated { token ->
            api.get("/v1/purchase-intents/$purchaseIntentId", token).toPurchaseIntent()
        }

    override suspend fun quote(
        purchaseIntentId: String,
        billing: BillingContact,
        idempotencyKey: String,
    ): PurchaseIntentDetails = authenticated { token ->
        api.post(
            path = "/v1/purchase-intents/$purchaseIntentId/quote",
            json = JSONObject().put("billing", billing.toJson()),
            accessToken = token,
            headers = mapOf("Idempotency-Key" to idempotencyKey),
            readTimeoutMillis = 90_000,
        ).toPurchaseIntent()
    }

    override suspend fun createApprovalSession(
        purchaseIntentId: String,
        idempotencyKey: String,
    ): PurchaseIntentDetails = authenticated { token ->
        api.post(
            path = "/v1/purchase-intents/$purchaseIntentId/prava-session",
            accessToken = token,
            headers = mapOf("Idempotency-Key" to idempotencyKey),
            readTimeoutMillis = 30_000,
        ).toPurchaseIntent()
    }

    override suspend fun reconcile(purchaseIntentId: String): PurchaseIntentDetails =
        authenticated { token ->
            api.post(
                path = "/v1/purchase-intents/$purchaseIntentId/reconcile",
                accessToken = token,
                readTimeoutMillis = 180_000,
            ).toPurchaseIntent()
        }

    override suspend fun getVerifiedResult(purchaseIntentId: String): VerifiedResult =
        authenticated { token ->
            val response = api.get("/v1/purchase-intents/$purchaseIntentId/receipt", token)
            val amount = Money(
                minorUnits = response.getLong("amount_minor"),
                currencyCode = response.requiredString("currency"),
            )
            when (response.requiredString("kind")) {
                "AUTHORIZATION_RESULT" -> VerifiedResult.AuthorizationDeclined(
                    purchaseIntentId = response.requiredString("purchase_intent_id"),
                    merchantName = response.requiredString("merchant_name"),
                    title = response.requiredString("title"),
                    amount = amount,
                    message = response.requiredString("message"),
                )

                "ORDER_RECEIPT" -> VerifiedResult.OrderReceipt(
                    purchaseIntentId = response.requiredString("purchase_intent_id"),
                    merchantName = response.requiredString("merchant_name"),
                    title = response.requiredString("title"),
                    amount = amount,
                    merchantOrderId = response.requiredString("merchant_order_id"),
                )

                else -> throw invalidResponse()
            }
        }

    override suspend fun saveMessage(purchaseIntentId: String, text: String) {
        authenticated { token ->
            api.post(
                path = "/v1/purchase-intents/$purchaseIntentId/message",
                json = JSONObject().put("text", text.trim()),
                accessToken = token,
            )
        }
    }

    override suspend fun fetch(occasionId: String): MandateDetails? = authenticated { token ->
        try {
            api.get("/v1/occasions/$occasionId/mandate", token).toMandateDetails()
        } catch (error: WishTraceApiException) {
            // No mandate armed yet is an expected, not-yet-set-up state.
            if (error.code == "MANDATE_NOT_FOUND" || error.code == "HTTP_404") {
                null
            } else {
                throw error
            }
        }
    }

    override suspend fun setup(occasionId: String, candidateId: String): MandateDetails =
        authenticated { token ->
            api.post(
                path = "/v1/occasions/$occasionId/mandate/setup",
                json = JSONObject().put("candidate_id", candidateId),
                accessToken = token,
                readTimeoutMillis = 30_000,
            ).toMandateDetails()
        }

    override suspend fun refresh(occasionId: String): MandateDetails = authenticated { token ->
        api.post(
            path = "/v1/occasions/$occasionId/mandate/refresh",
            accessToken = token,
            readTimeoutMillis = 30_000,
        ).toMandateDetails()
    }

    override suspend fun execute(
        occasionId: String,
        billing: BillingContact,
        idempotencyKey: String,
    ): MandateDetails = authenticated { token ->
        api.post(
            path = "/v1/occasions/$occasionId/mandate/execute",
            json = JSONObject().put("billing", billing.toJson()),
            accessToken = token,
            headers = mapOf("Idempotency-Key" to idempotencyKey),
            readTimeoutMillis = 180_000,
        ).toMandateDetails()
    }

    private suspend fun <T> authenticated(block: suspend (String) -> T): T {
        val token = sessionStore.current()?.accessToken
            ?: throw WishTraceApiException(
                message = "Sign in again to continue.",
                code = "AUTHENTICATION_REQUIRED",
                recoverable = true,
            )
        return try {
            block(token)
        } catch (error: WishTraceApiException) {
            if (error.code == "AUTHENTICATION_REQUIRED") sessionStore.clear()
            throw error
        }
    }
}

private fun JSONObject.toCandidate(merchantId: String, merchantName: String): ProductCandidate =
    ProductCandidate(
        id = requiredString("id"),
        merchantId = merchantId,
        merchantName = merchantName,
        title = requiredString("title"),
        currentPrice = Money(getLong("price_minor"), requiredString("currency")),
        productUrl = requiredString("product_url"),
        checkoutReference = optionalString("merchant_variant_id"),
        availability = AvailabilityState.valueOf(requiredString("availability")),
        requiredVariant = if (isNull("merchant_variant_id")) "Merchant variant" else null,
        selectedVariant = optionalString("variant_title"),
        supportedDeliveryFact = null,
        arrivesBy = null,
        sourceTimestamp = requiredInstant("source_timestamp"),
        sourceMode = SourceMode.LIVE,
    )

private fun JSONObject.toDecision(rejections: List<CandidateRejection>): RankedDecision {
    val rationalesJson = getJSONArray("rationales")
    val rationales = buildList {
        for (index in 0 until rationalesJson.length()) {
            val rationale = rationalesJson.getJSONObject(index)
            add(
                CandidateRationale(
                    candidateId = rationale.requiredString("candidate_id"),
                    evidenceIds = rationale.getJSONArray("evidence_ids").strings(),
                    reason = rationale.requiredString("reason"),
                ),
            )
        }
    }
    return RankedDecision(
        selectedCandidateId = requiredString("selected_candidate_id"),
        alternativeCandidateIds = getJSONArray("alternative_candidate_ids").strings(),
        rationales = rationales,
        rejections = rejections,
        uncertainty = RankingUncertainty.valueOf(requiredString("uncertainty")),
        modelRequestId = optionalString("model_request_id"),
        promptVersion = requiredString("prompt_version"),
    )
}

private fun JSONObject.toPurchaseIntent(): PurchaseIntentDetails {
    val currency = requiredString("currency")
    val approval = if (isNull("approval_session")) {
        null
    } else {
        getJSONObject("approval_session").let {
            ApprovalSession(
                id = it.requiredString("session_id"),
                hostedUrl = it.requiredString("hosted_url"),
                expiresAt = it.requiredInstant("expires_at"),
            )
        }
    }
    return PurchaseIntentDetails(
        id = requiredString("id"),
        recipientId = requiredString("recipient_id"),
        occasionId = requiredString("occasion_id"),
        candidateId = requiredString("candidate_id"),
        state = TransactionState.valueOf(requiredString("state")),
        merchantName = requiredString("merchant_name"),
        merchantUrl = requiredString("merchant_url"),
        title = requiredString("title"),
        variantTitle = optionalString("variant_title"),
        itemPrice = Money(getLong("item_price_minor"), currency),
        approvedTotal = if (isNull("approved_total_minor")) {
            null
        } else {
            Money(getLong("approved_total_minor"), currency)
        },
        deliverySummary = optionalString("delivery_summary"),
        quoteExpiresAt = optionalInstant("quote_expires_at"),
        approvalSession = approval,
        providerStatus = optionalString("provider_status"),
        merchantOutcome = optionalString("merchant_outcome"),
        merchantOrderId = optionalString("merchant_order_id"),
        updatedAt = requiredInstant("updated_at"),
    )
}

private fun JSONObject.toMandateDetails(): MandateDetails {
    val approvalUrl = if (isNull("approval_session")) {
        null
    } else {
        getJSONObject("approval_session").optionalString("hosted_url")
    }
    val charges = getJSONArray("charges")
    val lastCharge = if (charges.length() == 0) null else {
        charges.getJSONObject(charges.length() - 1)
    }
    return MandateDetails(
        id = requiredString("id"),
        recipientId = requiredString("recipient_id"),
        occasionId = requiredString("occasion_id"),
        status = MandateStatus.fromWire(requiredString("state")),
        approvedAmountMinor = getInt("approved_amount_minor"),
        currency = requiredString("currency"),
        recurringFrequency = requiredString("recurring_frequency"),
        merchantScope = requiredString("merchant_scope"),
        maxCharges = getInt("max_charges"),
        chargesUsed = getInt("charges_used"),
        merchantName = requiredString("merchant_name"),
        productTitle = requiredString("product_title"),
        itemPriceMinor = getInt("item_price_minor"),
        approvalUrl = approvalUrl,
        lastProviderStatus = optionalString("provider_status"),
        setupFailureCode = optionalString("setup_failure_code"),
        merchantOrderId = optionalString("merchant_order_id"),
        merchantOutcome = optionalString("merchant_outcome")
            ?.let(MandateMerchantOutcome::fromWire),
        visaConfirmation = optionalString("visa_confirmation")
            ?.let(MandateVisaConfirmation::fromWire),
        lastChargeState = lastCharge?.requiredString("state"),
        lastChargeAmountMinor = lastCharge?.getInt("amount_minor"),
        lastChargeFailureCode = lastCharge?.optionalString("failure_code"),
        createdAt = requiredInstant("created_at"),
        updatedAt = requiredInstant("updated_at"),
    )
}

private fun BillingContact.toJson(): JSONObject = JSONObject()
    .put("email", email)
    .put("first_name", firstName)
    .put("last_name", lastName)
    .put("address_line1", addressLine1)
    .put("address_line2", addressLine2 ?: JSONObject.NULL)
    .put("city", city)
    .put("region", region ?: JSONObject.NULL)
    .put("postal_code", postalCode)
    .put("country_code", countryCode)
    .put("phone", phone ?: JSONObject.NULL)

private fun rejectionReason(code: String): CandidateRejectionReason = when (code) {
    "UNSUPPORTED_CHECKOUT" -> CandidateRejectionReason.UNSUPPORTED_MERCHANT
    "UNAVAILABLE" -> CandidateRejectionReason.UNAVAILABLE
    "MISSING_VARIANT" -> CandidateRejectionReason.MISSING_VARIANT
    "OVER_BUDGET" -> CandidateRejectionReason.OVER_BUDGET
    "EXPLICIT_DISLIKE" -> CandidateRejectionReason.EXCLUDED
    "RECENTLY_ATTEMPTED" -> CandidateRejectionReason.RECENTLY_ATTEMPTED
    else -> throw invalidResponse()
}

private fun JSONArray.strings(): List<String> = buildList {
    for (index in 0 until length()) add(getString(index))
}

private fun JSONArray.ids(key: String): List<String> = buildList {
    for (index in 0 until length()) add(getJSONObject(index).requiredString(key))
}

private fun JSONObject.optionalInstant(key: String): Instant? =
    if (isNull(key)) null else requiredInstant(key)

private fun invalidResponse(): WishTraceApiException = WishTraceApiException(
    message = "WishTrace received an incomplete server response.",
    code = "INVALID_SERVER_RESPONSE",
    recoverable = true,
)
