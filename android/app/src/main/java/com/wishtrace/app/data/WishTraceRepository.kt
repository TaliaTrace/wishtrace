package com.wishtrace.app.data

import com.wishtrace.app.domain.HomeSnapshot
import com.wishtrace.app.domain.Money
import com.wishtrace.app.domain.Occasion
import com.wishtrace.app.domain.OccasionKind
import com.wishtrace.app.domain.Recipient
import java.time.LocalDate
import java.time.ZoneId

interface WishTraceRepository {
    suspend fun getHome(): HomeSnapshot?

    suspend fun reset()
}

data class RecipientInput(
    val id: String?,
    val displayName: String,
    val relationship: String,
    val photoUri: String?,
    val interests: List<String>,
    val dislikes: List<String>,
    val hint: String?,
)

data class OccasionInput(
    val id: String?,
    val recipientId: String,
    val kind: OccasionKind,
    val localDate: LocalDate,
    val timeZone: ZoneId,
    val budget: Money,
    val requiredArrivalDate: LocalDate?,
)

interface PeopleRepository {
    suspend fun saveRecipient(input: RecipientInput): Recipient
}

interface OccasionRepository {
    suspend fun saveOccasion(input: OccasionInput): Occasion
}
