package com.wishtrace.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wishtrace.app.data.PeopleRepository
import com.wishtrace.app.domain.Recipient
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface PeopleUiState {
    data object Loading : PeopleUiState
    data object Empty : PeopleUiState
    data class Content(val recipients: List<Recipient>) : PeopleUiState
    data class Error(val message: String) : PeopleUiState
}

class PeopleViewModel(
    private val repository: PeopleRepository,
) : ViewModel() {
    private val mutableState = MutableStateFlow<PeopleUiState>(PeopleUiState.Loading)
    val state: StateFlow<PeopleUiState> = mutableState.asStateFlow()

    private var loadJob: Job? = null

    init {
        retry()
    }

    fun retry() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            mutableState.value = PeopleUiState.Loading
            mutableState.value = try {
                val recipients = repository.listRecipients()
                if (recipients.isEmpty()) {
                    PeopleUiState.Empty
                } else {
                    PeopleUiState.Content(recipients)
                }
            } catch (_: Exception) {
                PeopleUiState.Error("We couldn't load your people. Try again.")
            }
        }
    }
}
