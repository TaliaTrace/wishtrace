package com.wishtrace.app.data

import com.wishtrace.app.domain.AppSession
import com.wishtrace.app.domain.GoogleAuthChallenge
import kotlinx.coroutines.flow.StateFlow

interface AuthRepository {
    val session: StateFlow<AppSession?>

    suspend fun createGoogleChallenge(): Result<GoogleAuthChallenge>

    /**
     * Sends a Google ID token to the WishTrace backend for verification.
     *
     * The Android client never treats a locally parsed Google token as an authoritative
     * WishTrace session.
     */
    suspend fun exchangeGoogleIdToken(
        challengeId: String,
        idToken: String,
    ): Result<AppSession>

    suspend fun logout(): Result<Unit>
}

class BackendAuthRepository(
    private val api: WishTraceApiClient,
    private val sessionStore: SessionStore,
) : AuthRepository {
    override val session: StateFlow<AppSession?> = sessionStore.session

    override suspend fun createGoogleChallenge(): Result<GoogleAuthChallenge> = runCatching {
        val body = api.post(path = "/v1/auth/google/challenge")
        GoogleAuthChallenge(
            id = body.requiredString("challenge_id"),
            nonce = body.requiredString("nonce"),
            expiresAt = body.requiredInstant("expires_at"),
        )
    }

    override suspend fun exchangeGoogleIdToken(
        challengeId: String,
        idToken: String,
    ): Result<AppSession> = runCatching {
        require(challengeId.isNotBlank())
        require(idToken.isNotBlank())
        val body = api.post(
            path = "/v1/auth/google/exchange",
            json = org.json.JSONObject()
                .put("challenge_id", challengeId)
                .put("id_token", idToken),
        )
        val user = body.getJSONObject("user")
        AppSession(
            accessToken = body.requiredString("access_token"),
            expiresAt = body.requiredInstant("expires_at"),
            user = com.wishtrace.app.domain.AppUser(
                id = user.requiredString("id"),
                displayName = user.requiredString("display_name"),
                email = user.optionalString("email"),
            ),
        ).also(sessionStore::save)
    }

    override suspend fun logout(): Result<Unit> {
        val accessToken = sessionStore.current()?.accessToken
        val result = if (accessToken == null) {
            Result.success(Unit)
        } else {
            runCatching {
                api.post(
                    path = "/v1/auth/logout",
                    accessToken = accessToken,
                )
                Unit
            }
        }
        sessionStore.clear()
        return result
    }
}
