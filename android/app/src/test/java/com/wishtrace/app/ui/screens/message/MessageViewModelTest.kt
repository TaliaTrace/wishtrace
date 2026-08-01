package com.wishtrace.app.ui.screens.message

import com.wishtrace.app.domain.MessageOrigin
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MessageViewModelTest {
    @Test
    fun generatedDraftIsMarkedEditedAfterUserChange() {
        val viewModel = MessageViewModel(
            initialText = "A generated draft",
            initialOrigin = MessageOrigin.GENERATED,
        )

        assertFalse(viewModel.state.value.wasEdited)
        viewModel.updateText("My edited note")

        assertTrue(viewModel.state.value.wasEdited)
    }

    @Test
    fun saveCreatesAUserReviewedMessage() {
        val createdAt = Instant.parse("2026-07-30T12:00:00Z")
        val viewModel = MessageViewModel(
            now = { createdAt },
            newId = { "message_1" },
        )
        viewModel.updateText("  Happy birthday, Sophie!  ")

        val result = viewModel.save("recipient_sophie")

        assertNotNull(result)
        requireNotNull(result)
        assertEquals("message_1", result.id)
        assertEquals("Happy birthday, Sophie!", result.text)
        assertEquals(MessageOrigin.USER, result.origin)
        assertEquals(createdAt, result.createdAt)
    }

    @Test
    fun blankMessageCanOnlyBeSkipped() {
        val viewModel = MessageViewModel()

        val result = viewModel.save("recipient_sophie")

        assertNull(result)
        assertEquals(
            "Write a note or skip this step.",
            viewModel.state.value.error,
        )
    }

    @Test
    fun inputIsBoundedToDomainLimit() {
        val viewModel = MessageViewModel()

        viewModel.updateText("x".repeat(MessageViewModel.MaxMessageLength + 30))

        assertEquals(
            MessageViewModel.MaxMessageLength,
            viewModel.state.value.text.length,
        )
    }
}
