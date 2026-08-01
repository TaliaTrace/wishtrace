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
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

object ControlledFixtures {
    val Today: LocalDate = LocalDate.of(2026, 7, 30)

    fun homeSnapshot(today: LocalDate = Today): HomeSnapshot {
        val recipient = Recipient(
            id = "recipient_sophie",
            displayName = "Sophie",
            relationship = "Close friend",
            initials = "S",
            interests = listOf("Cozy gaming", "Live music", "Books"),
            dislikes = listOf("Decorative clutter"),
            hints = listOf(
                HintEvidence(
                    id = "hint_weekend_games",
                    text = "She has been replaying cozy games on quiet weekends.",
                    sourceLabel = "Saved note",
                    savedOn = today.minusDays(14),
                ),
            ),
        )

        val occasion = Occasion(
            id = "occasion_sophie_birthday",
            recipientId = recipient.id,
            kind = OccasionKind.BIRTHDAY,
            localDate = today.plusDays(12),
            timeZone = ZoneId.of("America/Los_Angeles"),
            budget = Money(minorUnits = 6_000, currencyCode = "USD"),
            requiredArrivalDate = today.plusDays(11),
        )

        return HomeSnapshot(
            recipient = recipient,
            occasion = occasion,
            today = today,
            sourceMode = SourceMode.CONTROLLED,
        )
    }
}

class SeededWishTraceRepository(
    private val today: LocalDate = ControlledFixtures.Today,
) : WishTraceRepository, PeopleRepository, OccasionRepository {
    private val mutex = Mutex()
    private var snapshot: HomeSnapshot? = ControlledFixtures.homeSnapshot(today)

    override suspend fun getHome(): HomeSnapshot? {
        delay(180)
        return mutex.withLock { snapshot }
    }

    override suspend fun saveRecipient(input: RecipientInput): Recipient {
        val normalizedName = input.displayName.trim()
        val recipient = Recipient(
            id = input.id ?: "recipient_${normalizedName.lowercase().replace(nonIdCharacters, "_")}",
            displayName = normalizedName,
            relationship = input.relationship.trim(),
            initials = normalizedName
                .split(Regex("\\s+"))
                .take(2)
                .mapNotNull { it.firstOrNull()?.uppercase() }
                .joinToString(""),
            photoUri = input.photoUri,
            interests = input.interests.map(String::trim).filter(String::isNotEmpty).distinct(),
            dislikes = input.dislikes.map(String::trim).filter(String::isNotEmpty).distinct(),
            hints = input.hint
                ?.trim()
                ?.takeIf(String::isNotEmpty)
                ?.let { hint ->
                    listOf(
                        HintEvidence(
                            id = "hint_local",
                            text = hint,
                            sourceLabel = "Saved note",
                            savedOn = today,
                        ),
                    )
                }
                .orEmpty(),
        )
        mutex.withLock {
            val current = snapshot
            snapshot = current?.copy(recipient = recipient)
        }
        return recipient
    }

    override suspend fun saveOccasion(input: OccasionInput): Occasion {
        val occasion = Occasion(
            id = input.id ?: "occasion_${input.recipientId}_${input.kind.name.lowercase()}",
            recipientId = input.recipientId,
            kind = input.kind,
            localDate = input.localDate,
            timeZone = input.timeZone,
            budget = input.budget,
            requiredArrivalDate = input.requiredArrivalDate,
        )
        mutex.withLock {
            val current = snapshot
            checkNotNull(current) { "A recipient must be saved before an occasion." }
            snapshot = current.copy(
                occasion = occasion,
                today = today,
                sourceMode = SourceMode.CONTROLLED,
            )
        }
        return occasion
    }

    suspend fun saveSetup(
        recipientInput: RecipientInput,
        occasionForRecipient: (Recipient) -> OccasionInput,
    ): HomeSnapshot {
        val recipient = saveRecipient(recipientInput)
        saveOccasion(occasionForRecipient(recipient))
        return checkNotNull(getHome())
    }

    override suspend fun reset() {
        mutex.withLock {
            snapshot = ControlledFixtures.homeSnapshot(today)
        }
    }

    private companion object {
        val nonIdCharacters = Regex("[^a-z0-9]+")
    }
}
