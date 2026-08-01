package com.wishtrace.app.domain

enum class SessionMode {
    LIVE,
    LOCAL,
}

data class AppUser(
    val id: String,
    val displayName: String,
    val email: String?,
)

data class AppSession(
    val mode: SessionMode,
    val user: AppUser,
)
