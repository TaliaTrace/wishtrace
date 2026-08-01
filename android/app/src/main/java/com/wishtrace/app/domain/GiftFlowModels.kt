package com.wishtrace.app.domain

import java.time.Instant
import java.time.LocalDate

enum class DiscoveryStage(
    val title: String,
    val explanation: String,
) {
    CHECKING_CATALOG(
        title = "Checking controlled catalog records",
        explanation = "Start with known records, never invented products.",
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
    val eligibleCandidateIds: List<String>,
    val sourceMode: SourceMode,
) {
    init {
        require(eligibleCandidateIds.isNotEmpty()) {
            "Discovery preparation requires at least one eligible candidate ID."
        }
        require(eligibleCandidateIds.none(String::isBlank)) {
            "Candidate IDs cannot be blank."
        }
        require(eligibleCandidateIds.distinct().size == eligibleCandidateIds.size) {
            "Candidate IDs must be unique."
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
    READY_FOR_APPROVAL,
    SESSION_CREATING,
    AWAITING_USER,
    PROCESSING,
    SUCCEEDED,
    DECLINED,
    CANCELLED,
    EXPIRED,
    FAILED,
    UNKNOWN,
}

data class PravaApprovalHandoff(
    val sessionId: String,
    val hostedApprovalUrl: String,
    val state: TransactionState,
)
