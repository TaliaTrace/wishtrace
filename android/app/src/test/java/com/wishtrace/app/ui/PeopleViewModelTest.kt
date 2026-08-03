package com.wishtrace.app.ui

import com.wishtrace.app.data.PeopleRepository
import com.wishtrace.app.data.RecipientInput
import com.wishtrace.app.domain.Recipient
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PeopleViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setMainDispatcher() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun resetMainDispatcher() {
        Dispatchers.resetMain()
    }

    @Test
    fun loadsEverySavedRecipient() = runTest(dispatcher) {
        val recipients = listOf(recipient("zaid", "Zaid"), recipient("talia", "Talia"))
        val viewModel = PeopleViewModel(FakePeopleRepository(recipients))

        advanceUntilIdle()

        assertEquals(PeopleUiState.Content(recipients), viewModel.state.value)
    }

    @Test
    fun emptyRepositoryShowsHonestEmptyState() = runTest(dispatcher) {
        val viewModel = PeopleViewModel(FakePeopleRepository(emptyList()))

        advanceUntilIdle()

        assertEquals(PeopleUiState.Empty, viewModel.state.value)
    }

    @Test
    fun retryRecoversAfterARequestFailure() = runTest(dispatcher) {
        val repository = FakePeopleRepository(error = IOException("offline"))
        val viewModel = PeopleViewModel(repository)
        advanceUntilIdle()
        assertTrue(viewModel.state.value is PeopleUiState.Error)

        val recovered = listOf(recipient("zaid", "Zaid"))
        repository.error = null
        repository.recipients = recovered
        viewModel.retry()
        advanceUntilIdle()

        assertEquals(PeopleUiState.Content(recovered), viewModel.state.value)
    }

    private fun recipient(id: String, name: String) = Recipient(
        id = id,
        displayName = name,
        relationship = "Sibling",
        initials = name.take(1),
        interests = listOf("Gaming"),
        dislikes = emptyList(),
        hints = emptyList(),
    )

    private class FakePeopleRepository(
        var recipients: List<Recipient> = emptyList(),
        var error: Exception? = null,
    ) : PeopleRepository {
        override suspend fun listRecipients(): List<Recipient> {
            error?.let { throw it }
            return recipients
        }

        override suspend fun saveRecipient(input: RecipientInput): Recipient =
            error("Save is not exercised by this test")
    }
}
