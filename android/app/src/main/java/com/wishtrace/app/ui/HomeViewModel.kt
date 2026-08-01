package com.wishtrace.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wishtrace.app.data.WishTraceRepository
import com.wishtrace.app.domain.HomeSnapshot
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface HomeUiState {
    data object Loading : HomeUiState

    data object Empty : HomeUiState

    data class Content(val snapshot: HomeSnapshot) : HomeUiState

    data class Error(val message: String) : HomeUiState
}

class HomeViewModel(
    private val repository: WishTraceRepository,
) : ViewModel() {
    private val mutableState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val state: StateFlow<HomeUiState> = mutableState.asStateFlow()

    private var loadJob: Job? = null

    init {
        load()
    }

    fun retry() {
        load()
    }

    private fun load() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            mutableState.value = HomeUiState.Loading
            mutableState.value = try {
                repository.getHome()
                    ?.let(HomeUiState::Content)
                    ?: HomeUiState.Empty
            } catch (_: Exception) {
                HomeUiState.Error(
                    message = "We couldn't load this profile. Try again.",
                )
            }
        }
    }
}
