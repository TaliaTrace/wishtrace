package com.wishtrace.app.domain

import java.time.Instant

data class AppUser(
    val id: String,
    val displayName: String,
    val email: String?,
)

data class AppSession(
    val accessToken: String,
    val expiresAt: Instant,
    val user: AppUser,
)

data class GoogleAuthChallenge(
    val id: String,
    val nonce: String,
    val expiresAt: Instant,
)
