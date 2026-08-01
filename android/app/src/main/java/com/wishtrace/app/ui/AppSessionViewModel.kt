package com.wishtrace.app.ui

import androidx.lifecycle.ViewModel
import com.wishtrace.app.domain.AppSession
import com.wishtrace.app.domain.AppUser
import com.wishtrace.app.domain.SessionMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AppSessionViewModel : ViewModel() {
    private val mutableSession = MutableStateFlow<AppSession?>(null)
    val session: StateFlow<AppSession?> = mutableSession.asStateFlow()

    fun enterLocal() {
        mutableSession.value = AppSession(
            mode = SessionMode.LOCAL,
            user = AppUser(
                id = "talia_local",
                displayName = "Talia",
                email = null,
            ),
        )
    }

    fun acceptVerifiedSession(session: AppSession) {
        require(session.mode == SessionMode.LIVE)
        mutableSession.value = session
    }

    fun signOut() {
        mutableSession.value = null
    }
}
