package com.wishtrace.app.data

import com.wishtrace.app.domain.AppSession

interface AuthRepository {
    /**
     * Sends a Google ID token to the WishTrace backend for verification.
     *
     * The Android client never treats a locally parsed Google token as an authoritative
     * WishTrace session.
     */
    suspend fun exchangeGoogleIdToken(idToken: String): Result<AppSession>
}

class BackendPendingAuthRepository : AuthRepository {
    override suspend fun exchangeGoogleIdToken(idToken: String): Result<AppSession> {
        require(idToken.isNotBlank())
        return Result.failure(
            IllegalStateException(
                "Google responded, but WishTrace backend verification is not connected yet.",
            ),
        )
    }
}
