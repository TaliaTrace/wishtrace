package com.wishtrace.app.data

import com.wishtrace.app.domain.HintEvidence
import com.wishtrace.app.domain.HomeSnapshot
import com.wishtrace.app.domain.Money
import com.wishtrace.app.domain.Occasion
import com.wishtrace.app.domain.OccasionKind
import com.wishtrace.app.domain.Recipient
import com.wishtrace.app.domain.SourceMode
import java.time.LocalDate
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
}
