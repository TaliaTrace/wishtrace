package com.wishtrace.app.domain

import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.assertThrows
import org.junit.Test

class GiftFlowModelsTest {
    @Test
    fun rankedDecisionRejectsMoreThanTwoAlternatives() {
        assertThrows(IllegalArgumentException::class.java) {
            RankedDecision(
                selectedCandidateId = "primary",
                alternativeCandidateIds = listOf("a", "b", "c"),
                rationales = listOf(
                    CandidateRationale(
                        candidateId = "primary",
                        evidenceIds = listOf("interest_gaming"),
                        reason = "Matches a supplied interest.",
                    ),
                ),
                rejections = emptyList(),
                uncertainty = RankingUncertainty.LOW,
                modelRequestId = null,
                promptVersion = "rank-v1",
            )
        }
    }

    @Test
    fun selectedCandidateCannotAlsoBeRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            RankedDecision(
                selectedCandidateId = "primary",
                alternativeCandidateIds = emptyList(),
                rationales = listOf(
                    CandidateRationale(
                        candidateId = "primary",
                        evidenceIds = listOf("interest_gaming"),
                        reason = "Matches a supplied interest.",
                    ),
                ),
                rejections = listOf(
                    CandidateRejection(
                        candidateId = "primary",
                        reason = CandidateRejectionReason.OVER_BUDGET,
                        explanation = "Over the approved budget.",
                    ),
                ),
                uncertainty = RankingUncertainty.LOW,
                modelRequestId = null,
                promptVersion = "rank-v1",
            )
        }
    }

    @Test
    fun productCandidateNeedsAResolvableSourceReference() {
        assertThrows(IllegalArgumentException::class.java) {
            ProductCandidate(
                id = "candidate",
                merchantId = "merchant",
                merchantName = "Merchant",
                title = "Sourced product",
                currentPrice = Money(4_000, "USD"),
                productUrl = null,
                checkoutReference = null,
                availability = AvailabilityState.UNKNOWN,
                requiredVariant = null,
                selectedVariant = null,
                supportedDeliveryFact = null,
                arrivesBy = LocalDate.of(2026, 8, 10),
                sourceTimestamp = Instant.parse("2026-07-30T12:00:00Z"),
                sourceMode = SourceMode.CONTROLLED,
            )
        }
    }

    @Test
    fun groundedDecisionRejectsUnknownEvidence() {
        val decision = RankedDecision(
            selectedCandidateId = "primary",
            alternativeCandidateIds = emptyList(),
            rationales = listOf(
                CandidateRationale(
                    candidateId = "primary",
                    evidenceIds = listOf("invented_evidence"),
                    reason = "Claims a supplied interest.",
                ),
            ),
            rejections = emptyList(),
            uncertainty = RankingUncertainty.MEDIUM,
            modelRequestId = "request_1",
            promptVersion = "rank-v1",
        )

        assertThrows(IllegalArgumentException::class.java) {
            decision.validatedAgainst(
                allCandidateIds = setOf("primary"),
                eligibleCandidateIds = setOf("primary"),
                evidenceIds = setOf("interest_gaming"),
            )
        }
    }
}
