package com.wishtrace.app.data

import android.app.Activity
import android.content.Context
import android.util.Base64
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import java.security.SecureRandom

class GoogleCredentialClient(context: Context) {
    private val manager = CredentialManager.create(context.applicationContext)

    suspend fun requestAuthorizedAccount(
        activity: Activity,
        webClientId: String,
    ): String {
        require(webClientId.isNotBlank()) { "Google sign-in is not configured for this build." }
        val option = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(true)
            .setAutoSelectEnabled(true)
            .setServerClientId(webClientId)
            .setNonce(generateNonce())
            .build()
        return requestToken(
            activity = activity,
            request = GetCredentialRequest.Builder()
                .addCredentialOption(option)
                .build(),
        )
    }

    suspend fun requestWithGoogleButton(
        activity: Activity,
        webClientId: String,
    ): String {
        require(webClientId.isNotBlank()) { "Google sign-in is not configured for this build." }
        val option = GetSignInWithGoogleOption.Builder(webClientId)
            .setNonce(generateNonce())
            .build()
        return requestToken(
            activity = activity,
            request = GetCredentialRequest.Builder()
                .addCredentialOption(option)
                .build(),
        )
    }

    suspend fun clearCredentialState() {
        manager.clearCredentialState(ClearCredentialStateRequest())
    }

    private suspend fun requestToken(
        activity: Activity,
        request: GetCredentialRequest,
    ): String {
        val credential = manager.getCredential(
            context = activity,
            request = request,
        ).credential
        require(
            credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL,
        ) {
            "Google returned an unsupported credential."
        }
        return GoogleIdTokenCredential
            .createFrom(credential.data)
            .idToken
    }

    private fun generateNonce(byteLength: Int = 32): String {
        val bytes = ByteArray(byteLength)
        SecureRandom().nextBytes(bytes)
        return Base64.encodeToString(
            bytes,
            Base64.NO_WRAP or Base64.URL_SAFE or Base64.NO_PADDING,
        )
    }
}
