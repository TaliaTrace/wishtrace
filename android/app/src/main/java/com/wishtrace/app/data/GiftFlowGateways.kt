package com.wishtrace.app.data

import com.wishtrace.app.domain.BillingContact
import com.wishtrace.app.domain.DiscoveryPreparation
import com.wishtrace.app.domain.DiscoveryStage
import com.wishtrace.app.domain.GiftDiscoveryRequest
import com.wishtrace.app.domain.GroundedRankingRequest
import com.wishtrace.app.domain.MandateDetails
import com.wishtrace.app.domain.PravaApprovalHandoff
import com.wishtrace.app.domain.PravaApprovalRequest
import com.wishtrace.app.domain.PurchaseIntentDetails
import com.wishtrace.app.domain.RankedDecision
import com.wishtrace.app.domain.VerifiedResult

/**
 * Android-facing boundaries for the WishTrace backend.
 *
 * Implementations must call the backend over HTTPS. OpenAI and Prava credentials never belong
 * in the Android client.
 */
interface GiftDiscoveryGateway {
    suspend fun prepareCandidates(
        request: GiftDiscoveryRequest,
        onStage: suspend (DiscoveryStage) -> Unit,
    ): DiscoveryPreparation
}

interface GroundedRankingGateway {
    suspend fun rankAllowedCandidates(
        request: GroundedRankingRequest,
    ): RankedDecision
}

interface PravaApprovalGateway {
    suspend fun createSandboxApproval(
        request: PravaApprovalRequest,
    ): PravaApprovalHandoff
}

interface PurchaseFlowGateway {
    suspend fun createIntent(candidateId: String): PurchaseIntentDetails

    suspend fun getIntent(purchaseIntentId: String): PurchaseIntentDetails

    suspend fun quote(
        purchaseIntentId: String,
        billing: BillingContact,
        idempotencyKey: String,
    ): PurchaseIntentDetails

    suspend fun createApprovalSession(
        purchaseIntentId: String,
        idempotencyKey: String,
    ): PurchaseIntentDetails

    suspend fun reconcile(purchaseIntentId: String): PurchaseIntentDetails

    suspend fun getVerifiedResult(purchaseIntentId: String): VerifiedResult

    suspend fun saveMessage(purchaseIntentId: String, text: String)
}

/**
 * The mandate autopilot boundary: set up delegated spend authority the owner
 * approves once, then charge/execute it later within the cap. The raw Prava
 * charge/report endpoints stay backend-only — Android never sees credentials.
 */
interface MandateGateway {
    suspend fun fetch(occasionId: String): MandateDetails?

    suspend fun setup(
        occasionId: String,
        candidateId: String,
        replaceUnknownMandateId: String? = null,
    ): MandateDetails

    suspend fun refresh(occasionId: String): MandateDetails

    suspend fun execute(
        occasionId: String,
        billing: BillingContact,
        idempotencyKey: String,
    ): MandateDetails
}
