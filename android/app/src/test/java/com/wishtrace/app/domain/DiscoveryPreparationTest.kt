package com.wishtrace.app.domain

import java.time.Instant
import org.junit.Assert.assertThrows
import org.junit.Test

class DiscoveryPreparationTest {
    @Test
    fun rejectsDuplicateCandidateIdsBeforeRanking() {
        assertThrows(IllegalArgumentException::class.java) {
            DiscoveryPreparation(
                discoveryId = "discovery",
                candidates = listOf(candidate("candidate_a")),
                decision = decision("candidate_a"),
                eligibleCandidateIds = listOf("candidate_a", "candidate_a"),
                sourceMode = SourceMode.LIVE,
            )
        }
    }

    private fun candidate(id: String) = ProductCandidate(
        id = id,
        merchantId = "merchant",
        merchantName = "Merchant",
        title = "Observed gift",
        currentPrice = Money(500, "USD"),
        productUrl = "https://example.com/product",
        checkoutReference = "variant",
        availability = AvailabilityState.AVAILABLE,
        requiredVariant = null,
        selectedVariant = "$5",
        supportedDeliveryFact = null,
        arrivesBy = null,
        sourceTimestamp = Instant.parse("2026-08-01T00:00:00Z"),
        sourceMode = SourceMode.LIVE,
    )

    private fun decision(id: String) = RankedDecision(
        selectedCandidateId = id,
        alternativeCandidateIds = emptyList(),
        rationales = listOf(
            CandidateRationale(id, listOf("interest"), "Matches the interest."),
        ),
        rejections = emptyList(),
        uncertainty = RankingUncertainty.LOW,
        modelRequestId = null,
        promptVersion = "test-v1",
    )
}
