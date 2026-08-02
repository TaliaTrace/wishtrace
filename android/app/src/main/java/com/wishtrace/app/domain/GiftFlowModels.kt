package com.wishtrace.app.domain

import java.time.Instant
import java.time.LocalDate

enum class DiscoveryStage(
    val title: String,
    val explanation: String,
) {
    CHECKING_CATALOG(
        title = "Checking the live gift catalog",
        explanation = "Read current products directly from the merchant.",
    ),
    APPLYING_BUDGET(
        title = "Applying the approved budget",
        explanation = "Remove options that exceed the amount you set.",
    ),
    CHECKING_FULFILLMENT(
        title = "Checking purchase requirements",
        explanation = "Review availability, variants and known delivery facts.",
    ),
    PREPARING_RANKING(
        title = "Preparing allowed candidate IDs",
        explanation = "Only eligible IDs may cross the grounded ranking boundary.",
    ),
}

data class GiftDiscoveryRequest(
    val recipientId: String,
    val occasionId: String,
    val budget: Money,
)

data class DiscoveryPreparation(
    val discoveryId: String,
    val candidates: List<ProductCandidate>,
    val decision: RankedDecision,
    val eligibleCandidateIds: List<String>,
    val sourceMode: SourceMode,
) {
    init {
        require(discoveryId.isNotBlank()) { "Discovery ID cannot be blank." }
        require(candidates.isNotEmpty()) { "Discovery requires observed candidates." }
        require(candidates.map { it.id }.distinct().size == candidates.size) {
            "Candidate IDs must be unique."
        }
        require(eligibleCandidateIds.isNotEmpty()) {
            "Discovery preparation requires at least one eligible candidate ID."
        }
        require(eligibleCandidateIds.none(String::isBlank)) {
            "Candidate IDs cannot be blank."
        }
        require(eligibleCandidateIds.distinct().size == eligibleCandidateIds.size) {
            "Candidate IDs must be unique."
        }
        require(eligibleCandidateIds.all { id -> candidates.any { it.id == id } }) {
            "Every eligible ID must identify an observed candidate."
        }
    }
}

data class GroundedRankingRequest(
    val recipientId: String,
    val occasionId: String,
    val allowedCandidateIds: List<String>,
) {
    init {
        require(recipientId.isNotBlank()) { "Recipient ID cannot be blank." }
        require(occasionId.isNotBlank()) { "Occasion ID cannot be blank." }
        require(allowedCandidateIds.isNotEmpty()) {
            "Grounded ranking requires at least one allowed candidate."
        }
        require(allowedCandidateIds.none(String::isBlank)) {
            "Allowed candidate IDs cannot be blank."
        }
        require(allowedCandidateIds.distinct().size == allowedCandidateIds.size) {
            "Allowed candidate IDs must be unique."
        }
    }
}

enum class AvailabilityState {
    AVAILABLE,
    UNAVAILABLE,
    UNKNOWN,
}

data class ProductCandidate(
    val id: String,
    val merchantId: String,
    val merchantName: String,
    val title: String,
    val currentPrice: Money,
    val productUrl: String?,
    val checkoutReference: String?,
    val availability: AvailabilityState,
    val requiredVariant: String?,
    val selectedVariant: String?,
    val supportedDeliveryFact: String?,
    val arrivesBy: LocalDate?,
    val sourceTimestamp: Instant,
    val sourceMode: SourceMode,
) {
    init {
        require(id.isNotBlank()) { "Candidate ID cannot be blank." }
        require(merchantId.isNotBlank()) { "Merchant ID cannot be blank." }
        require(merchantName.isNotBlank()) { "Merchant name cannot be blank." }
        require(title.isNotBlank()) { "Candidate title cannot be blank." }
        require(currentPrice.minorUnits >= 0) { "Candidate price cannot be negative." }
        require(!productUrl.isNullOrBlank() || !checkoutReference.isNullOrBlank()) {
            "A product URL or checkout reference is required."
        }
    }
}

enum class CandidateRejectionReason {
    UNSUPPORTED_MERCHANT,
    UNAVAILABLE,
    MISSING_VARIANT,
    OVER_BUDGET,
    LATE_DELIVERY,
    EXCLUDED,
    RECENTLY_ATTEMPTED,
}

data class CandidateRejection(
    val candidateId: String,
    val reason: CandidateRejectionReason,
    val explanation: String,
) {
    init {
        require(candidateId.isNotBlank()) { "Rejected candidate ID cannot be blank." }
        require(explanation.isNotBlank()) { "Rejection explanation cannot be blank." }
    }
}

data class CandidateRationale(
    val candidateId: String,
    val evidenceIds: List<String>,
    val reason: String,
) {
    init {
        require(candidateId.isNotBlank()) { "Rationale candidate ID cannot be blank." }
        require(evidenceIds.isNotEmpty()) { "A ranking rationale needs evidence." }
        require(evidenceIds.none(String::isBlank)) { "Evidence IDs cannot be blank." }
        require(reason.isNotBlank()) { "A ranking reason cannot be blank." }
    }
}

enum class RankingUncertainty {
    LOW,
    MEDIUM,
    HIGH,
}

data class RankedDecision(
    val selectedCandidateId: String,
    val alternativeCandidateIds: List<String>,
    val rationales: List<CandidateRationale>,
    val rejections: List<CandidateRejection>,
    val uncertainty: RankingUncertainty,
    val modelRequestId: String?,
    val promptVersion: String,
) {
    init {
        require(selectedCandidateId.isNotBlank()) { "Selected candidate ID cannot be blank." }
        require(alternativeCandidateIds.size <= 2) {
            "WishTrace shows at most two alternatives."
        }
        require(alternativeCandidateIds.none(String::isBlank)) {
            "Alternative candidate IDs cannot be blank."
        }
        require(alternativeCandidateIds.distinct().size == alternativeCandidateIds.size) {
            "Alternative candidate IDs must be unique."
        }
        require(selectedCandidateId !in alternativeCandidateIds) {
            "Selected candidate cannot also be an alternative."
        }
        val rankedIds = setOf(selectedCandidateId) + alternativeCandidateIds
        require(rationales.map { it.candidateId }.toSet() == rankedIds) {
            "Every ranked candidate needs exactly one grounded rationale."
        }
        require(rationales.map { it.candidateId }.distinct().size == rationales.size) {
            "Ranked candidate rationales must be unique."
        }
        require(rejections.map { it.candidateId }.distinct().size == rejections.size) {
            "Candidate rejections must be unique."
        }
        require(rejections.none { it.candidateId in rankedIds }) {
            "A ranked candidate cannot also be rejected."
        }
        require(promptVersion.isNotBlank()) { "Prompt version cannot be blank." }
    }

    fun validatedAgainst(
        allCandidateIds: Set<String>,
        eligibleCandidateIds: Set<String>,
        evidenceIds: Set<String>,
    ): RankedDecision {
        val rankedIds = setOf(selectedCandidateId) + alternativeCandidateIds
        require(rankedIds.all { it in eligibleCandidateIds }) {
            "Ranking referenced a candidate that deterministic checks did not allow."
        }
        require(rankedIds.all { it in allCandidateIds }) {
            "Ranking referenced an unknown candidate."
        }
        require(rejections.all { it.candidateId in allCandidateIds }) {
            "A rejection referenced an unknown candidate."
        }
        require(rationales.flatMap { it.evidenceIds }.all { it in evidenceIds }) {
            "Ranking referenced unknown evidence."
        }
        return this
    }
}

data class PurchaseIntent(
    val id: String,
    val recipientId: String,
    val occasionId: String,
    val candidateId: String,
    val approvedSnapshotAmount: Money,
    val selectedVariant: String?,
    val createdAt: Instant,
    val state: TransactionState = TransactionState.DRAFT,
) {
    init {
        require(id.isNotBlank()) { "Purchase intent ID cannot be blank." }
        require(recipientId.isNotBlank()) { "Recipient ID cannot be blank." }
        require(occasionId.isNotBlank()) { "Occasion ID cannot be blank." }
        require(candidateId.isNotBlank()) { "Candidate ID cannot be blank." }
        require(approvedSnapshotAmount.minorUnits >= 0) {
            "Approved snapshot amount cannot be negative."
        }
    }
}

enum class MessageOrigin {
    USER,
    GENERATED,
}

data class PersonalMessage(
    val id: String,
    val recipientId: String,
    val text: String,
    val origin: MessageOrigin,
    val wasEdited: Boolean,
    val createdAt: Instant,
) {
    init {
        require(id.isNotBlank()) { "Message ID cannot be blank." }
        require(recipientId.isNotBlank()) { "Recipient ID cannot be blank." }
        require(text.isNotBlank()) { "A saved personal message cannot be blank." }
        require(text.length <= 500) { "Personal messages are limited to 500 characters." }
    }
}

data class PravaApprovalRequest(
    val purchaseIntentId: String,
    val approvedCandidateId: String,
    val approvedAmount: Money,
    val idempotencyKey: String,
)

enum class TransactionState {
    DRAFT,
    VALIDATING,
    QUOTED,
    READY_FOR_APPROVAL,
    SESSION_CREATING,
    AWAITING_USER,
    CREDENTIALS_READY,
    CHECKOUT_IN_PROGRESS,
    ORDER_VERIFIED,
    SUCCEEDED,
    DECLINED,
    CANCELLED,
    EXPIRED,
    FAILED,
    UNKNOWN,
    RECONCILING,
}

data class PravaApprovalHandoff(
    val sessionId: String,
    val hostedApprovalUrl: String,
    val state: TransactionState,
)

data class BillingContact(
    val email: String,
    val firstName: String,
    val lastName: String,
    val addressLine1: String,
    val addressLine2: String? = null,
    val city: String,
    val region: String? = null,
    val postalCode: String,
    val countryCode: String = "US",
    val phone: String? = null,
) {
    init {
        require(email.isNotBlank()) { "Billing email cannot be blank." }
        require(firstName.isNotBlank()) { "First name cannot be blank." }
        require(lastName.isNotBlank()) { "Last name cannot be blank." }
        require(addressLine1.isNotBlank()) { "Billing address cannot be blank." }
        require(city.isNotBlank()) { "City cannot be blank." }
        require(postalCode.isNotBlank()) { "Postal code cannot be blank." }
        require(countryCode.matches(Regex("[A-Z]{2}"))) {
            "Country code must be a two-letter ISO code."
        }
    }
}

data class ApprovalSession(
    val id: String,
    val hostedUrl: String,
    val expiresAt: Instant,
)

data class PurchaseIntentDetails(
    val id: String,
    val recipientId: String,
    val occasionId: String,
    val candidateId: String,
    val state: TransactionState,
    val merchantName: String,
    val merchantUrl: String,
    val title: String,
    val variantTitle: String?,
    val itemPrice: Money,
    val approvedTotal: Money?,
    val deliverySummary: String?,
    val quoteExpiresAt: Instant?,
    val approvalSession: ApprovalSession?,
    val providerStatus: String?,
    val merchantOutcome: String?,
    val merchantOrderId: String?,
    val updatedAt: Instant,
)

sealed interface VerifiedResult {
    val purchaseIntentId: String
    val merchantName: String
    val title: String
    val amount: Money

    data class AuthorizationDeclined(
        override val purchaseIntentId: String,
        override val merchantName: String,
        override val title: String,
        override val amount: Money,
        val message: String,
    ) : VerifiedResult

    data class OrderReceipt(
        override val purchaseIntentId: String,
        override val merchantName: String,
        override val title: String,
        override val amount: Money,
        val merchantOrderId: String,
    ) : VerifiedResult
}
