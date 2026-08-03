package com.wishtrace.app.data

import android.content.Context
import android.net.Uri
import android.provider.ContactsContract
import java.io.File
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** A user-selected contact is imported locally; WishTrace never scans the address book. */
data class ImportedContact(
    val displayName: String,
    val localPhotoUri: String?,
)

class RecipientPhotoStore(context: Context) {
    private val preferences = context.getSharedPreferences(
        "wishtrace_recipient_photos",
        Context.MODE_PRIVATE,
    )

    fun photoUri(recipientId: String): String? = preferences.getString(recipientId, null)

    fun remember(recipientId: String, localPhotoUri: String?) {
        if (localPhotoUri == null) return
        preferences.edit().putString(recipientId, localPhotoUri).apply()
    }
}

suspend fun importChosenContact(
    context: Context,
    contactUri: Uri,
): ImportedContact? = withContext(Dispatchers.IO) {
    val resolver = context.contentResolver
    val contactData = resolver.query(
        contactUri,
        arrayOf(
            ContactsContract.Contacts.DISPLAY_NAME_PRIMARY,
            ContactsContract.Contacts.PHOTO_THUMBNAIL_URI,
        ),
        null,
        null,
        null,
    )?.use { cursor ->
        if (!cursor.moveToFirst()) return@use null
        val name = cursor.getString(0)?.trim()?.takeIf(String::isNotEmpty)
            ?: return@use null
        name to cursor.getString(1)
    } ?: return@withContext null
    val (displayName, thumbnailUri) = contactData

    val localPhotoUri = runCatching {
        val input = thumbnailUri
            ?.let(Uri::parse)
            ?.let(resolver::openInputStream)
            ?: ContactsContract.Contacts.openContactPhotoInputStream(
                resolver,
                contactUri,
                true,
            )
        input?.use { photoInput ->
            val directory = File(context.filesDir, "recipient_photos").apply { mkdirs() }
            val target = File(directory, "${UUID.randomUUID()}.jpg")
            target.outputStream().use { output -> photoInput.copyTo(output) }
            Uri.fromFile(target).toString()
        }
    }.getOrNull()

    ImportedContact(
        displayName = displayName,
        localPhotoUri = localPhotoUri,
    )
}
