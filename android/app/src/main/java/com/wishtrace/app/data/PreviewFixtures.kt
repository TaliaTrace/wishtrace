package com.wishtrace.app.data

import com.wishtrace.app.domain.HintEvidence
import com.wishtrace.app.domain.AvailabilityState
import com.wishtrace.app.domain.CandidateRationale
import com.wishtrace.app.domain.DiscoveryPreparation
import com.wishtrace.app.domain.HomeSnapshot
import com.wishtrace.app.domain.Money
import com.wishtrace.app.domain.Occasion
import com.wishtrace.app.domain.OccasionKind
import com.wishtrace.app.domain.Recipient
import com.wishtrace.app.domain.ProductCandidate
import com.wishtrace.app.domain.RankedDecision
import com.wishtrace.app.domain.RankingUncertainty
import com.wishtrace.app.domain.SourceMode
import java.time.LocalDate
import java.time.Instant
import java.time.ZoneId

/** Static values used only by Compose @Preview functions. Never wire this object at runtime. */
object PreviewFixtures {
    private val today: LocalDate = LocalDate.of(2026, 7, 30)

    fun homeSnapshot(): HomeSnapshot {
        val recipient = Recipient(
            id = "preview_recipient",
            displayName = "Sophie",
            relationship = "Close friend",
            initials = "S",
            interests = listOf("Cozy gaming", "Live music", "Books"),
            dislikes = listOf("Decorative clutter"),
            hints = listOf(
                HintEvidence(
                    id = "preview_hint",
                    text = "She has been replaying cozy games on quiet weekends.",
                    sourceLabel = "Saved note",
                    savedOn = today.minusDays(14),
                ),
            ),
        )
        return HomeSnapshot(
            recipient = recipient,
            occasion = Occasion(
                id = "preview_occasion",
                recipientId = recipient.id,
                kind = OccasionKind.BIRTHDAY,
                localDate = today.plusDays(12),
                timeZone = ZoneId.of("America/Los_Angeles"),
                budget = Money(minorUnits = 6_000, currencyCode = "USD"),
                requiredArrivalDate = today.plusDays(11),
            ),
            today = today,
            sourceMode = SourceMode.LIVE,
        )
    }

    fun discoveryPreparation(): DiscoveryPreparation {
        val candidate = ProductCandidate(
            id = "preview_candidate",
            merchantId = "preview_merchant",
            merchantName = "Preview merchant",
            title = "Observed gift",
            currentPrice = Money(5_000, "USD"),
            productUrl = "https://example.com/preview-only",
            checkoutReference = "preview_variant",
            availability = AvailabilityState.AVAILABLE,
            requiredVariant = null,
            selectedVariant = "$50",
            supportedDeliveryFact = null,
            arrivesBy = null,
            sourceTimestamp = Instant.parse("2026-07-30T12:00:00Z"),
            sourceMode = SourceMode.LIVE,
        )
        return DiscoveryPreparation(
            discoveryId = "preview_discovery",
            candidates = listOf(candidate),
            decision = RankedDecision(
                selectedCandidateId = candidate.id,
                alternativeCandidateIds = emptyList(),
                rationales = listOf(
                    CandidateRationale(
                        candidateId = candidate.id,
                        evidenceIds = listOf("preview_interest"),
                        reason = "Matches a saved interest.",
                    ),
                ),
                rejections = emptyList(),
                uncertainty = RankingUncertainty.LOW,
                modelRequestId = null,
                promptVersion = "preview-only",
            ),
            eligibleCandidateIds = listOf(candidate.id),
            sourceMode = SourceMode.LIVE,
        )
    }
}
