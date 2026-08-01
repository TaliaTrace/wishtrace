package com.wishtrace.app.ui.screens.message

import androidx.lifecycle.ViewModel
import com.wishtrace.app.domain.MessageOrigin
import com.wishtrace.app.domain.PersonalMessage
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class MessageUiState(
    val text: String = "",
    val origin: MessageOrigin = MessageOrigin.USER,
    val wasEdited: Boolean = false,
    val error: String? = null,
)

class MessageViewModel(
    initialText: String = "",
    initialOrigin: MessageOrigin = MessageOrigin.USER,
    private val now: () -> Instant = Instant::now,
    private val newId: () -> String = { UUID.randomUUID().toString() },
) : ViewModel() {
    private val mutableState = MutableStateFlow(
        MessageUiState(
            text = initialText.take(MaxMessageLength),
            origin = initialOrigin,
        ),
    )
    val state: StateFlow<MessageUiState> = mutableState.asStateFlow()

    fun updateText(value: String) {
        mutableState.update { current ->
            current.copy(
                text = value.take(MaxMessageLength),
                wasEdited = current.wasEdited ||
                    (current.origin == MessageOrigin.GENERATED && value != current.text),
                error = null,
            )
        }
    }

    fun save(recipientId: String): PersonalMessage? {
        val current = mutableState.value
        val normalized = current.text.trim()
        if (normalized.isBlank()) {
            mutableState.update { it.copy(error = "Write a note or skip this step.") }
            return null
        }
        return PersonalMessage(
            id = newId(),
            recipientId = recipientId,
            text = normalized,
            origin = current.origin,
            wasEdited = current.wasEdited,
            createdAt = now(),
        )
    }

    companion object {
        const val MaxMessageLength = 500
    }
}
