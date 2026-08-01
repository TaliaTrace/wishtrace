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

data class Recipient(
    val id: String,
    val displayName: String,
    val relationship: String,
    val initials: String,
    val photoUri: String? = null,
    val interests: List<String>,
    val dislikes: List<String>,
    val hints: List<HintEvidence>,
)

enum class OccasionKind(val displayName: String) {
    BIRTHDAY("Birthday"),
}

data class Occasion(
    val id: String,
    val recipientId: String,
    val kind: OccasionKind,
    val localDate: LocalDate,
    val timeZone: ZoneId,
    val budget: Money,
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
