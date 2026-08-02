package com.wishtrace.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wishtrace.app.data.MandateGateway
import com.wishtrace.app.data.WishTraceRepository
import com.wishtrace.app.domain.HomeSnapshot
import com.wishtrace.app.domain.MandateDetails
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface HomeUiState {
    data object Loading : HomeUiState

    data object Empty : HomeUiState

    /**
     * The occasion snapshot plus its reconciled autopilot [mandate], if one exists.
     * The mandate is fetched best-effort — a failure leaves it null so the home never
     * fails to load just because the mandate lookup did.
     */
    data class Content(
        val snapshot: HomeSnapshot,
        val mandate: MandateDetails? = null,
    ) : HomeUiState

    data class Error(val message: String) : HomeUiState
}

class HomeViewModel(
    private val repository: WishTraceRepository,
    private val mandateGateway: MandateGateway,
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
                val snapshot = repository.getHome()
                if (snapshot == null) {
                    HomeUiState.Empty
                } else {
                    val mandate = runCatching {
                        mandateGateway.fetch(snapshot.occasion.id)
                    }.getOrNull()
                    HomeUiState.Content(snapshot = snapshot, mandate = mandate)
                }
            } catch (_: Exception) {
                HomeUiState.Error(
                    message = "We couldn't load this profile. Try again.",
                )
            }
        }
    }
}
