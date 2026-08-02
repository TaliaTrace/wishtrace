package com.wishtrace.app.ui.screens.giftdna

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wishtrace.app.data.OccasionInput
import com.wishtrace.app.data.OccasionRepository
import com.wishtrace.app.data.PeopleRepository
import com.wishtrace.app.data.RecipientInput
import com.wishtrace.app.domain.Money
import com.wishtrace.app.domain.OccasionKind
import com.wishtrace.app.domain.PersonalityTraits
import com.wishtrace.app.domain.RecurringFrequency
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** The four gamified onboarding tiles, in order. */
enum class GiftDnaTile {
    // 🔴 Who's this for?
    RED,

    // 🔵 What's the moment?
    BLUE,

    // 🟢 What are they like?
    GREEN,

    // 🟡 How much, how often?
    YELLOW,
    ;

    val next: GiftDnaTile?
        get() = entries.getOrNull(ordinal + 1)

    val previous: GiftDnaTile?
        get() = entries.getOrNull(ordinal - 1)
}

/**
 * Relationship options for the Red tile. Each maps to the free-text relationship the
 * backend already accepts; the emoji is display-only.
 */
enum class RelationshipChoice(val label: String, val emoji: String) {
    PARTNER("Partner", "💕"),
    FAMILY("Family", "👨‍👩‍👧"),
    FRIEND("Friend", "🧑‍🤝‍🧑"),
    COLLEAGUE("Colleague", "💼"),
}

/**
 * Age band choices for the Red tile, mirroring the backend `AgeBand` literals. `null`
 * wire value means the owner skipped it, which the backend permits.
 */
enum class AgeBandChoice(val wire: String, val label: String) {
    CHILD("child", "Child"),
    TEEN("teen", "Teen"),
    YOUNG_ADULT("young_adult", "Young adult"),
    ADULT("adult", "Adult"),
    SENIOR("senior", "Senior"),
}

/** Green-tile axis endpoints. Each pole carries its backend wire value + display face. */
enum class EnergyChoice(val wire: String, val label: String, val emoji: String) {
    COMPETITIVE("competitive", "Competitive", "⚔️"),
    CHILL("chill", "Chill", "🌿"),
}

enum class EnvironmentChoice(val wire: String, val label: String, val emoji: String) {
    SCREENS("screens", "Screens", "📱"),
    OUTDOORS("outdoors", "Outdoors", "🏔️"),
}

enum class StyleChoice(val wire: String, val label: String, val emoji: String) {
    TRENDY("trendy", "Trendy", "✨"),
    NOSTALGIC("nostalgic", "Nostalgic", "📻"),
}

data class GiftDnaUiState(
    val tile: GiftDnaTile = GiftDnaTile.RED,
    // Red
    val displayName: String = "",
    val relationship: String = "",
    val ageBand: AgeBandChoice? = null,
    // Blue
    val occasionDate: LocalDate? = null,
    // Green
    val energy: EnergyChoice? = null,
    val environment: EnvironmentChoice? = null,
    val style: StyleChoice? = null,
    // Yellow
    val budgetMinorUnits: Long = DEFAULT_BUDGET_MINOR,
    val frequency: RecurringFrequency = RecurringFrequency.ONE_TIME,
    // Errors + progress
    val nameError: String? = null,
    val relationshipError: String? = null,
    val dateError: String? = null,
    val saveError: String? = null,
    val saving: Boolean = false,
    val saveCompleted: Boolean = false,
) {
    val personalityTraits: PersonalityTraits
        get() = PersonalityTraits(
            energy = energy?.wire,
            environment = environment?.wire,
            style = style?.wire,
        )

    val canAdvance: Boolean
        get() = !saving

    companion object {
        const val DEFAULT_BUDGET_MINOR = 2_500L
        const val MIN_BUDGET_MINOR = 500L
        const val MAX_BUDGET_MINOR = 5_000L
    }
}

class GiftDnaViewModel(
    private val peopleRepository: PeopleRepository,
    private val occasionRepository: OccasionRepository,
    private val today: LocalDate,
) : ViewModel() {
    private val mutableState = MutableStateFlow(GiftDnaUiState())
    val state: StateFlow<GiftDnaUiState> = mutableState.asStateFlow()

    fun updateName(value: String) = update {
        copy(displayName = value, nameError = null, saveError = null)
    }

    fun selectRelationship(choice: RelationshipChoice) = update {
        copy(relationship = choice.label, relationshipError = null, saveError = null)
    }

    fun selectAgeBand(choice: AgeBandChoice) = update {
        // Tapping the selected band again clears it (age band is optional).
        copy(ageBand = if (ageBand == choice) null else choice, saveError = null)
    }

    fun updateDate(value: LocalDate) = update {
        copy(occasionDate = value, dateError = null, saveError = null)
    }

    fun selectEnergy(choice: EnergyChoice) = update {
        copy(energy = if (energy == choice) null else choice, saveError = null)
    }

    fun selectEnvironment(choice: EnvironmentChoice) = update {
        copy(environment = if (environment == choice) null else choice, saveError = null)
    }

    fun selectStyle(choice: StyleChoice) = update {
        copy(style = if (style == choice) null else choice, saveError = null)
    }

    fun updateBudget(minorUnits: Long) = update {
        copy(
            budgetMinorUnits = minorUnits.coerceIn(
                GiftDnaUiState.MIN_BUDGET_MINOR,
                GiftDnaUiState.MAX_BUDGET_MINOR,
            ),
            saveError = null,
        )
    }

    fun selectFrequency(value: RecurringFrequency) = update {
        copy(frequency = value, saveError = null)
    }

    /** Advances to the next tile, validating only the tiles that gate progress. */
    fun advance() {
        val current = mutableState.value
        if (current.saving) return
        when (current.tile) {
            GiftDnaTile.RED -> {
                val nameError = if (current.displayName.trim().isEmpty()) {
                    "Enter their name"
                } else {
                    null
                }
                val relationshipError = if (current.relationship.trim().isEmpty()) {
                    "Choose who they are to you"
                } else {
                    null
                }
                mutableState.update {
                    it.copy(nameError = nameError, relationshipError = relationshipError)
                }
                if (nameError == null && relationshipError == null) moveTo(GiftDnaTile.BLUE)
            }

            GiftDnaTile.BLUE -> {
                val dateError = when {
                    current.occasionDate == null -> "Choose the date"
                    current.occasionDate.isBefore(today) -> "Choose today or later"
                    else -> null
                }
                mutableState.update { it.copy(dateError = dateError) }
                if (dateError == null) moveTo(GiftDnaTile.GREEN)
            }

            // Green is entirely optional — no validation gates it.
            GiftDnaTile.GREEN -> moveTo(GiftDnaTile.YELLOW)

            // Yellow always has a valid slider value + frequency; finishing saves.
            GiftDnaTile.YELLOW -> save()
        }
    }

    fun back() {
        val current = mutableState.value
        if (current.saving) return
        current.tile.previous?.let { moveTo(it) }
    }

    private fun save() {
        val current = mutableState.value
        viewModelScope.launch {
            mutableState.update { it.copy(saving = true, saveError = null) }
            runCatching {
                val recipient = peopleRepository.saveRecipient(
                    RecipientInput(
                        id = null,
                        displayName = current.displayName.trim(),
                        relationship = current.relationship.trim(),
                        // Green replaces the interests form — send none.
                        interests = emptyList(),
                        dislikes = emptyList(),
                        personalityTraits = current.personalityTraits
                            .takeUnless { it.isEmpty },
                        ageBand = current.ageBand?.wire,
                        hint = null,
                    ),
                )
                occasionRepository.saveOccasion(
                    OccasionInput(
                        id = null,
                        recipientId = recipient.id,
                        kind = OccasionKind.BIRTHDAY,
                        localDate = requireNotNull(current.occasionDate),
                        timeZone = ZoneId.systemDefault(),
                        budget = Money(
                            minorUnits = current.budgetMinorUnits,
                            currencyCode = "USD",
                        ),
                        recurringFrequency = current.frequency,
                        requiredArrivalDate = null,
                    ),
                )
            }.onSuccess {
                mutableState.update { it.copy(saving = false, saveCompleted = true) }
            }.onFailure {
                mutableState.update {
                    it.copy(
                        saving = false,
                        saveError = "We couldn't save this. Try again.",
                    )
                }
            }
        }
    }

    private fun moveTo(tile: GiftDnaTile) {
        mutableState.update { it.copy(tile = tile, saveError = null) }
    }

    private inline fun update(transform: GiftDnaUiState.() -> GiftDnaUiState) {
        mutableState.update(transform)
    }

    companion object {
        fun datePickerUtcMillisToLocalDate(utcMillis: Long): LocalDate =
            Instant.ofEpochMilli(utcMillis)
                .atZone(ZoneOffset.UTC)
                .toLocalDate()
    }
}
