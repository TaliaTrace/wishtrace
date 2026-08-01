package com.wishtrace.app.data

import com.wishtrace.app.domain.SourceMode
import com.wishtrace.app.domain.Money
import com.wishtrace.app.domain.OccasionKind
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class SeededWishTraceRepositoryTest {
    @Test
    fun exposesControlledProfileWithTwelveDayBoundary() = runBlocking {
        val snapshot = requireNotNull(SeededWishTraceRepository(
            today = LocalDate.of(2026, 7, 30),
        ).getHome())

        assertEquals(SourceMode.CONTROLLED, snapshot.sourceMode)
        assertEquals("Sophie", snapshot.recipient.displayName)
        assertEquals(12, snapshot.daysUntil)
        assertEquals(6_000, snapshot.occasion.budget.minorUnits)
    }

    @Test
    fun savedContextReplacesTheControlledFixtureAtomicallyForHome() = runBlocking {
        val repository = SeededWishTraceRepository(
            today = LocalDate.of(2026, 7, 30),
        )
        val recipient = repository.saveRecipient(
            RecipientInput(
                id = "recipient_sophie",
                displayName = "Maya Chen",
                relationship = "Sibling",
                photoUri = "content://photo/1",
                interests = listOf("Books", "Travel"),
                dislikes = listOf("Clutter"),
                hint = "She bookmarked a coastal train route.",
            ),
        )
        repository.saveOccasion(
            OccasionInput(
                id = "occasion_sophie_birthday",
                recipientId = recipient.id,
                kind = OccasionKind.BIRTHDAY,
                localDate = LocalDate.of(2026, 9, 3),
                timeZone = ZoneId.of("Asia/Karachi"),
                budget = Money(7_500, "USD"),
                requiredArrivalDate = LocalDate.of(2026, 9, 2),
            ),
        )

        val snapshot = requireNotNull(repository.getHome())
        assertEquals("Maya Chen", snapshot.recipient.displayName)
        assertEquals("content://photo/1", snapshot.recipient.photoUri)
        assertEquals(7_500, snapshot.occasion.budget.minorUnits)
        assertEquals(LocalDate.of(2026, 9, 3), snapshot.occasion.localDate)
    }
}
