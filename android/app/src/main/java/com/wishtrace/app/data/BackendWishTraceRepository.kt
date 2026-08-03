package com.wishtrace.app.data

import com.wishtrace.app.domain.HintEvidence
import com.wishtrace.app.domain.HomeSnapshot
import com.wishtrace.app.domain.Money
import com.wishtrace.app.domain.Occasion
import com.wishtrace.app.domain.OccasionKind
import com.wishtrace.app.domain.PersonalityTraits
import com.wishtrace.app.domain.Recipient
import com.wishtrace.app.domain.RecurringFrequency
import com.wishtrace.app.domain.SourceMode
import java.time.LocalDate
import java.time.ZoneId
import org.json.JSONArray
import org.json.JSONObject

class BackendWishTraceRepository(
    private val api: WishTraceApiClient,
    private val sessionStore: SessionStore,
    private val recipientPhotoStore: RecipientPhotoStore? = null,
) : WishTraceRepository, PeopleRepository, OccasionRepository {
    override suspend fun getHome(): HomeSnapshot? = authenticated { token ->
        val body = api.get(path = "/v1/home", accessToken = token)
        if (body.isNull("recipient") || body.isNull("occasion")) return@authenticated null
        HomeSnapshot(
            recipient = body.getJSONObject("recipient").toRecipient(recipientPhotoStore),
            occasion = body.getJSONObject("occasion").toOccasion(),
            today = LocalDate.parse(body.requiredString("today")),
            sourceMode = SourceMode.LIVE,
        )
    }

    override suspend fun saveRecipient(input: RecipientInput): Recipient = authenticated { token ->
        val request = JSONObject()
            .put("display_name", input.displayName.trim())
            .put("relationship", input.relationship.trim())
            .put("interests", JSONArray(input.interests))
            .put("dislikes", JSONArray(input.dislikes))
            .put("hint", input.hint?.trim()?.takeIf(String::isNotEmpty) ?: JSONObject.NULL)
        // personality_traits is a nested object of only the axes the owner set; the
        // backend defaults it when omitted, so send it only when there is a signal.
        input.personalityTraits?.takeUnless { it.isEmpty }?.let { traits ->
            val traitsJson = JSONObject()
            traits.energy?.let { traitsJson.put("energy", it) }
            traits.environment?.let { traitsJson.put("environment", it) }
            traits.style?.let { traitsJson.put("style", it) }
            request.put("personality_traits", traitsJson)
        }
        input.ageBand?.let { request.put("age_band", it) }
        val response = if (input.id == null) {
            api.post(
                path = "/v1/recipients",
                json = request,
                accessToken = token,
            )
        } else {
            api.put(
                path = "/v1/recipients/${input.id}",
                json = request,
                accessToken = token,
            )
        }
        response.toRecipient(recipientPhotoStore)
    }

    override suspend fun listRecipients(): List<Recipient> = authenticated { token ->
        val body = api.getArray(path = "/v1/recipients", accessToken = token)
        buildList {
            for (index in 0 until body.length()) {
                add(body.getJSONObject(index).toRecipient(recipientPhotoStore))
            }
        }
    }

    override suspend fun saveOccasion(input: OccasionInput): Occasion = authenticated { token ->
        val request = JSONObject()
            .put("recipient_id", input.recipientId)
            .put("kind", input.kind.name)
            .put("local_date", input.localDate.toString())
            .put("time_zone", input.timeZone.id)
            .put("budget_minor", input.budget.minorUnits)
            .put("currency", input.budget.currencyCode)
            .put("recurring_frequency", input.recurringFrequency.wire)
            .put(
                "required_arrival_date",
                input.requiredArrivalDate?.toString() ?: JSONObject.NULL,
            )
        val response = if (input.id == null) {
            api.post(
                path = "/v1/occasions",
                json = request,
                accessToken = token,
            )
        } else {
            api.put(
                path = "/v1/occasions/${input.id}",
                json = request,
                accessToken = token,
            )
        }
        response.toOccasion()
    }

    override suspend fun listOccasions(recipientId: String?): List<Occasion> =
        authenticated { token ->
            val path = recipientId?.let { "/v1/occasions?recipient_id=$it" } ?: "/v1/occasions"
            val body = api.getArray(path = path, accessToken = token)
            buildList {
                for (index in 0 until body.length()) {
                    add(body.getJSONObject(index).toOccasion())
                }
            }
        }

    private suspend fun <T> authenticated(block: suspend (String) -> T): T {
        val token = sessionStore.current()?.accessToken
            ?: throw WishTraceApiException(
                message = "Sign in again to continue.",
                code = "AUTHENTICATION_REQUIRED",
                recoverable = true,
            )
        return try {
            block(token)
        } catch (error: WishTraceApiException) {
            if (error.code == "AUTHENTICATION_REQUIRED") {
                sessionStore.clear()
            }
            throw error
        }
    }
}

private fun JSONObject.toRecipient(photoStore: RecipientPhotoStore? = null): Recipient {
    val interests = getJSONArray("interests").toStrings()
    val dislikes = getJSONArray("dislikes").toStrings()
    val hintsJson = getJSONArray("hints")
    val hints = buildList {
        for (index in 0 until hintsJson.length()) {
            val hint = hintsJson.getJSONObject(index)
            add(
                HintEvidence(
                    id = hint.requiredString("id"),
                    text = hint.requiredString("text"),
                    sourceLabel = hint.requiredString("source_label"),
                    savedOn = LocalDate.parse(hint.requiredString("saved_on")),
                ),
            )
        }
    }
    val personality = if (isNull("personality_traits")) {
        null
    } else {
        getJSONObject("personality_traits").toPersonalityTraits()
    }
    return Recipient(
        id = requiredString("id"),
        displayName = requiredString("display_name"),
        relationship = requiredString("relationship"),
        initials = requiredString("initials"),
        photoUri = photoStore?.photoUri(requiredString("id")),
        interests = interests,
        dislikes = dislikes,
        personalityTraits = personality?.takeUnless(PersonalityTraits::isEmpty),
        ageBand = optionalString("age_band"),
        hints = hints,
    )
}

private fun JSONObject.toPersonalityTraits(): PersonalityTraits = PersonalityTraits(
    energy = optionalString("energy"),
    environment = optionalString("environment"),
    style = optionalString("style"),
)

private fun JSONObject.toOccasion(): Occasion = Occasion(
    id = requiredString("id"),
    recipientId = requiredString("recipient_id"),
    kind = OccasionKind.valueOf(requiredString("kind")),
    localDate = LocalDate.parse(requiredString("local_date")),
    timeZone = ZoneId.of(requiredString("time_zone")),
    budget = Money(
        minorUnits = getLong("budget_minor"),
        currencyCode = requiredString("currency"),
    ),
    recurringFrequency = RecurringFrequency.fromWire(requiredString("recurring_frequency")),
    requiredArrivalDate = if (isNull("required_arrival_date")) {
        null
    } else {
        LocalDate.parse(requiredString("required_arrival_date"))
    },
)

private fun JSONArray.toStrings(): List<String> = buildList {
    for (index in 0 until length()) {
        add(getString(index))
    }
}
