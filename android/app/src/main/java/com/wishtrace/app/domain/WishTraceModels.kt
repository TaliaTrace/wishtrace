package com.wishtrace.app.domain

import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

enum class SourceMode {
    LIVE,
}

data class HintEvidence(
    val id: String,
    val text: String,
    val sourceLabel: String,
    val savedOn: LocalDate,
)

/**
 * Green-tile "Gift DNA": three binary axes, all optional. Captured from three
 * either/or taps (no typing) rather than a free-text interests form. Any axis may
 * be null when the owner skipped it. Mirrors the backend `personality_traits`
 * object `{energy, environment, style}`.
 */
data class PersonalityTraits(
    // ⚔️ Competitive ↔ 🌿 Chill
    val energy: String? = null,
    // 📱 Screens ↔ 🏔️ Outdoors
    val environment: String? = null,
    // ✨ Trendy ↔ 📻 Nostalgic
    val style: String? = null,
) {
    val isEmpty: Boolean = energy == null && environment == null && style == null

    /** Human-readable labels for the axes the owner actually set, for display. */
    fun asChips(): List<String> = buildList {
        energy?.let { add(it.replaceFirstChar(Char::uppercase)) }
        environment?.let { add(it.replaceFirstChar(Char::uppercase)) }
        style?.let { add(it.replaceFirstChar(Char::uppercase)) }
    }
}

data class Recipient(
    val id: String,
    val displayName: String,
    val relationship: String,
    val initials: String,
    val photoUri: String? = null,
    val interests: List<String>,
    val dislikes: List<String>,
    val personalityTraits: PersonalityTraits? = null,
    val ageBand: String? = null,
    val hints: List<HintEvidence>,
)

enum class OccasionKind(val displayName: String) {
    BIRTHDAY("Birthday"),
}

/** Yellow-tile toggle: "Just this once" vs "Every year, automatically". */
enum class RecurringFrequency(val wire: String, val displayName: String) {
    ONE_TIME("one_time", "Just this once"),
    YEARLY("yearly", "Every year, automatically"),
    ;

    companion object {
        fun fromWire(value: String): RecurringFrequency =
            entries.firstOrNull { it.wire == value } ?: ONE_TIME
    }
}

data class Occasion(
    val id: String,
    val recipientId: String,
    val kind: OccasionKind,
    val localDate: LocalDate,
    val timeZone: ZoneId,
    val budget: Money,
    val recurringFrequency: RecurringFrequency = RecurringFrequency.ONE_TIME,
    val requiredArrivalDate: LocalDate?,
) {
    fun daysUntil(today: LocalDate): Int =
        ChronoUnit.DAYS.between(today, localDate).toInt().coerceAtLeast(0)
}

data class HomeSnapshot(
    val recipient: Recipient,
    val occasion: Occasion,
    val today: LocalDate,
    val sourceMode: SourceMode,
) {
    val daysUntil: Int = occasion.daysUntil(today)
}
