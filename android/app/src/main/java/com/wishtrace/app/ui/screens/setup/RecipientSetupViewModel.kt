package com.wishtrace.app.ui.screens.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wishtrace.app.data.OccasionInput
import com.wishtrace.app.data.OccasionRepository
import com.wishtrace.app.data.PeopleRepository
import com.wishtrace.app.data.RecipientInput
import com.wishtrace.app.domain.HomeSnapshot
import com.wishtrace.app.domain.Money
import com.wishtrace.app.domain.OccasionKind
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class RecipientSetupStep {
    PERSON,
    OCCASION,
}

data class RecipientSetupUiState(
    val step: RecipientSetupStep = RecipientSetupStep.PERSON,
    val displayName: String = "",
    val relationship: String = "",
    val photoUri: String? = null,
    val occasionDate: LocalDate? = null,
    val selectedInterests: Set<String> = emptySet(),
    val dislikesText: String = "",
    val budgetText: String = "",
    val hintText: String = "",
    val nameError: String? = null,
    val relationshipError: String? = null,
    val dateError: String? = null,
    val interestsError: String? = null,
    val budgetError: String? = null,
    val saveError: String? = null,
    val saving: Boolean = false,
    val saveCompleted: Boolean = false,
)

data class PersonValidation(
    val nameError: String? = null,
    val relationshipError: String? = null,
) {
    val isValid: Boolean = nameError == null && relationshipError == null
}

data class OccasionValidation(
    val dateError: String? = null,
    val interestsError: String? = null,
    val budgetError: String? = null,
    val budgetMinorUnits: Long? = null,
) {
    val isValid: Boolean =
        dateError == null &&
            interestsError == null &&
            budgetError == null &&
            budgetMinorUnits != null
}

object RecipientSetupValidator {
    fun validatePerson(name: String, relationship: String): PersonValidation =
        PersonValidation(
            nameError = if (name.trim().isEmpty()) "Enter their name" else null,
            relationshipError = if (relationship.trim().isEmpty()) {
                "Choose or enter a relationship"
            } else {
                null
            },
        )

    fun validateOccasion(
        date: LocalDate?,
        interests: Set<String>,
        budgetText: String,
        today: LocalDate,
    ): OccasionValidation {
        val budgetMinorUnits = parseUsdMinorUnits(budgetText)
        return OccasionValidation(
            dateError = when {
                date == null -> "Choose a date"
                date.isBefore(today) -> "Choose today or a future date"
                else -> null
            },
            interestsError = if (interests.isEmpty()) "Pick at least one interest" else null,
            budgetError = when {
                budgetText.isBlank() -> "Enter a budget"
                budgetMinorUnits == null -> "Use a valid amount"
                budgetMinorUnits <= 0 -> "Budget must be more than zero"
                else -> null
            },
            budgetMinorUnits = budgetMinorUnits,
        )
    }

    fun parseUsdMinorUnits(value: String): Long? = runCatching {
        val normalized = value.trim().removePrefix("$").trim()
        BigDecimal(normalized)
            .setScale(2, RoundingMode.UNNECESSARY)
            .movePointRight(2)
            .longValueExact()
    }.getOrNull()

    fun datePickerUtcMillisToLocalDate(utcMillis: Long): LocalDate =
        Instant.ofEpochMilli(utcMillis)
            .atZone(ZoneOffset.UTC)
            .toLocalDate()
}

class RecipientSetupViewModel(
    private val peopleRepository: PeopleRepository,
    private val occasionRepository: OccasionRepository,
    private val today: LocalDate,
    private val initialSnapshot: HomeSnapshot?,
    initialStep: RecipientSetupStep = RecipientSetupStep.PERSON,
) : ViewModel() {
    private val initialState = initialSnapshot.toSetupState(initialStep)
    private val mutableState = MutableStateFlow(initialState)
    val state: StateFlow<RecipientSetupUiState> = mutableState.asStateFlow()

    fun updateName(value: String) = update {
        copy(displayName = value, nameError = null, saveError = null)
    }

    fun updateRelationship(value: String) = update {
        copy(relationship = value, relationshipError = null, saveError = null)
    }

    fun updatePhoto(uri: String?) = update {
        copy(photoUri = uri, saveError = null)
    }

    fun updateDate(value: LocalDate) = update {
        copy(occasionDate = value, dateError = null, saveError = null)
    }

    fun toggleInterest(value: String) = update {
        val next = selectedInterests.toMutableSet().apply {
            if (!add(value)) remove(value)
        }
        copy(selectedInterests = next, interestsError = null, saveError = null)
    }

    fun updateDislikes(value: String) = update {
        copy(dislikesText = value, saveError = null)
    }

    fun updateBudget(value: String) = update {
        copy(budgetText = value, budgetError = null, saveError = null)
    }

    fun updateHint(value: String) = update {
        copy(hintText = value, saveError = null)
    }

    fun continueToOccasion() {
        val current = mutableState.value
        val validation = RecipientSetupValidator.validatePerson(
            name = current.displayName,
            relationship = current.relationship,
        )
        mutableState.update {
            it.copy(
                nameError = validation.nameError,
                relationshipError = validation.relationshipError,
            )
        }
        if (validation.isValid) {
            mutableState.update { it.copy(step = RecipientSetupStep.OCCASION) }
        }
    }

    fun backToPerson() {
        mutableState.update {
            it.copy(step = RecipientSetupStep.PERSON, saveError = null)
        }
    }

    fun save() {
        val current = mutableState.value
        val personValidation = RecipientSetupValidator.validatePerson(
            name = current.displayName,
            relationship = current.relationship,
        )
        val occasionValidation = RecipientSetupValidator.validateOccasion(
            date = current.occasionDate,
            interests = current.selectedInterests,
            budgetText = current.budgetText,
            today = today,
        )
        mutableState.update {
            it.copy(
                nameError = personValidation.nameError,
                relationshipError = personValidation.relationshipError,
                dateError = occasionValidation.dateError,
                interestsError = occasionValidation.interestsError,
                budgetError = occasionValidation.budgetError,
            )
        }
        if (!personValidation.isValid || !occasionValidation.isValid) return

        viewModelScope.launch {
            mutableState.update { it.copy(saving = true, saveError = null) }
            runCatching {
                val recipient = peopleRepository.saveRecipient(
                    RecipientInput(
                        id = initialSnapshot?.recipient?.id,
                        displayName = current.displayName,
                        relationship = current.relationship,
                        photoUri = current.photoUri,
                        interests = current.selectedInterests.sorted(),
                        dislikes = splitTags(current.dislikesText),
                        hint = current.hintText.takeIf(String::isNotBlank),
                    ),
                )
                occasionRepository.saveOccasion(
                    OccasionInput(
                        id = initialSnapshot?.occasion?.id,
                        recipientId = recipient.id,
                        kind = OccasionKind.BIRTHDAY,
                        localDate = requireNotNull(current.occasionDate),
                        timeZone = initialSnapshot?.occasion?.timeZone ?: ZoneId.systemDefault(),
                        budget = Money(
                            minorUnits = requireNotNull(occasionValidation.budgetMinorUnits),
                            currencyCode = "USD",
                        ),
                        requiredArrivalDate = current.occasionDate.minusDays(1),
                    ),
                )
            }.onSuccess {
                mutableState.update {
                    it.copy(saving = false, saveCompleted = true)
                }
            }.onFailure {
                mutableState.update {
                    it.copy(
                        saving = false,
                        saveError = "We couldn't save these details. Try again.",
                    )
                }
            }
        }
    }

    private inline fun update(transform: RecipientSetupUiState.() -> RecipientSetupUiState) {
        mutableState.update(transform)
    }

    private companion object {
        fun splitTags(value: String): List<String> = value
            .split(",")
            .map(String::trim)
            .filter(String::isNotEmpty)
            .distinct()

        fun HomeSnapshot?.toSetupState(initialStep: RecipientSetupStep): RecipientSetupUiState {
            val snapshot = this ?: return RecipientSetupUiState(step = initialStep)
            val currencyDigits = java.util.Currency
                .getInstance(snapshot.occasion.budget.currencyCode)
                .defaultFractionDigits
            val budget = BigDecimal.valueOf(
                snapshot.occasion.budget.minorUnits,
                currencyDigits,
            ).stripTrailingZeros().toPlainString()
            return RecipientSetupUiState(
                step = initialStep,
                displayName = snapshot.recipient.displayName,
                relationship = snapshot.recipient.relationship,
                photoUri = snapshot.recipient.photoUri,
                occasionDate = snapshot.occasion.localDate,
                selectedInterests = snapshot.recipient.interests.toSet(),
                dislikesText = snapshot.recipient.dislikes.joinToString(", "),
                budgetText = budget,
                hintText = snapshot.recipient.hints.firstOrNull()?.text.orEmpty(),
            )
        }
    }
}
