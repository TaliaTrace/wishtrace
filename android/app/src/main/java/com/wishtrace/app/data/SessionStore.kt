package com.wishtrace.app.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.wishtrace.app.domain.AppSession
import com.wishtrace.app.domain.AppUser
import java.security.KeyStore
import java.time.Instant
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject

class SessionStore(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
    private val mutableSession = MutableStateFlow(readSession())
    val session: StateFlow<AppSession?> = mutableSession.asStateFlow()

    @Synchronized
    fun save(session: AppSession) {
        require(session.accessToken.isNotBlank())
        val payload = JSONObject()
            .put("access_token", session.accessToken)
            .put("expires_at", session.expiresAt.toString())
            .put("user_id", session.user.id)
            .put("display_name", session.user.displayName)
            .put("email", session.user.email ?: JSONObject.NULL)
            .toString()
            .encodeToByteArray()
        val cipher = Cipher.getInstance(TRANSFORMATION).apply {
            init(Cipher.ENCRYPT_MODE, secretKey())
        }
        val encrypted = cipher.doFinal(payload)
        preferences.edit()
            .putString(IV, Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
            .putString(CIPHERTEXT, Base64.encodeToString(encrypted, Base64.NO_WRAP))
            .apply()
        mutableSession.value = session
    }

    @Synchronized
    fun current(): AppSession? {
        val value = mutableSession.value
        if (value != null && !value.expiresAt.isAfter(Instant.now())) {
            clear()
            return null
        }
        return value
    }

    @Synchronized
    fun clear() {
        preferences.edit().clear().apply()
        mutableSession.value = null
    }

    private fun readSession(): AppSession? = runCatching {
        val iv = preferences.getString(IV, null) ?: return null
        val ciphertext = preferences.getString(CIPHERTEXT, null) ?: return null
        val cipher = Cipher.getInstance(TRANSFORMATION).apply {
            init(
                Cipher.DECRYPT_MODE,
                secretKey(),
                GCMParameterSpec(128, Base64.decode(iv, Base64.NO_WRAP)),
            )
        }
        val body = JSONObject(
            cipher.doFinal(Base64.decode(ciphertext, Base64.NO_WRAP)).decodeToString(),
        )
        val session = AppSession(
            accessToken = body.requiredString("access_token"),
            expiresAt = body.requiredInstant("expires_at"),
            user = AppUser(
                id = body.requiredString("user_id"),
                displayName = body.requiredString("display_name"),
                email = body.optionalString("email"),
            ),
        )
        session.takeIf { it.expiresAt.isAfter(Instant.now()) }
    }.getOrElse {
        preferences.edit().clear().apply()
        null
    }

    private fun secretKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            "AndroidKeyStore",
        ).run {
            init(
                KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build(),
            )
            generateKey()
        }
    }

    private companion object {
        const val PREFERENCES = "wishtrace_session"
        const val KEY_ALIAS = "wishtrace_session_v1"
        const val IV = "iv"
        const val CIPHERTEXT = "ciphertext"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
    }
}
