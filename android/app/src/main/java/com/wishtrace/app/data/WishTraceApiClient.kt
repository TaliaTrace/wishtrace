package com.wishtrace.app.data

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.time.Instant
import java.time.OffsetDateTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

class WishTraceApiClient(baseUrl: String) {
    private val root = baseUrl.trimEnd('/').also(::requireSafeBaseUrl)

    suspend fun post(
        path: String,
        json: JSONObject? = null,
        accessToken: String? = null,
    ): JSONObject = withContext(Dispatchers.IO) {
        execute(
            method = "POST",
            path = path,
            json = json,
            accessToken = accessToken,
        )
    }

    suspend fun put(
        path: String,
        json: JSONObject,
        accessToken: String,
    ): JSONObject = withContext(Dispatchers.IO) {
        execute(
            method = "PUT",
            path = path,
            json = json,
            accessToken = accessToken,
        )
    }

    suspend fun get(
        path: String,
        accessToken: String,
    ): JSONObject = withContext(Dispatchers.IO) {
        execute(
            method = "GET",
            path = path,
            json = null,
            accessToken = accessToken,
        )
    }

    private fun execute(
        method: String,
        path: String,
        json: JSONObject?,
        accessToken: String?,
    ): JSONObject {
        require(path.startsWith('/'))
        val connection = URL(root + path).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = method
            connection.connectTimeout = 10_000
            connection.readTimeout = 15_000
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("X-Correlation-ID", java.util.UUID.randomUUID().toString())
            if (accessToken != null) {
                connection.setRequestProperty("Authorization", "Bearer $accessToken")
            }
            if (json != null) {
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                connection.outputStream.bufferedWriter(Charsets.UTF_8).use { writer ->
                    writer.write(json.toString())
                }
            }

            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            if (status !in 200..299) {
                throw apiFailure(status, text)
            }
            return if (text.isBlank()) JSONObject() else JSONObject(text)
        } catch (error: WishTraceApiException) {
            throw error
        } catch (error: IOException) {
            throw WishTraceApiException(
                message = "WishTrace could not reach the server. Check your connection and try again.",
                code = "NETWORK_UNAVAILABLE",
                recoverable = true,
                cause = error,
            )
        } finally {
            connection.disconnect()
        }
    }
}

class WishTraceApiException(
    override val message: String,
    val code: String,
    val recoverable: Boolean,
    cause: Throwable? = null,
) : Exception(message, cause)

private fun apiFailure(status: Int, text: String): WishTraceApiException {
    val body = runCatching { JSONObject(text) }.getOrNull()
    return WishTraceApiException(
        message = body?.optionalString("message")
            ?: if (status >= 500) {
                "WishTrace is temporarily unavailable. Try again."
            } else {
                "WishTrace could not complete that request."
            },
        code = body?.optionalString("code") ?: "HTTP_$status",
        recoverable = body?.optBoolean("recoverable", status >= 500) ?: (status >= 500),
    )
}

internal fun JSONObject.requiredString(key: String): String =
    getString(key).takeIf { it.isNotBlank() }
        ?: throw WishTraceApiException(
            message = "WishTrace received an incomplete server response.",
            code = "INVALID_SERVER_RESPONSE",
            recoverable = true,
        )

internal fun JSONObject.optionalString(key: String): String? =
    if (isNull(key)) null else optString(key).takeIf { it.isNotBlank() }

internal fun JSONObject.requiredInstant(key: String): Instant {
    val raw = requiredString(key)
    return runCatching { OffsetDateTime.parse(raw).toInstant() }
        .getOrElse {
            throw WishTraceApiException(
                message = "WishTrace received an invalid server timestamp.",
                code = "INVALID_SERVER_RESPONSE",
                recoverable = true,
                cause = it,
            )
        }
}

private fun requireSafeBaseUrl(baseUrl: String) {
    val uri = runCatching { URI(baseUrl) }.getOrNull()
        ?: throw IllegalArgumentException("WishTrace backend URL is invalid.")
    val local = uri.host == "127.0.0.1" || uri.host == "localhost"
    require(uri.scheme == "https" || (uri.scheme == "http" && local)) {
        "WishTrace backend must use HTTPS outside local development."
    }
}
