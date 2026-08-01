package com.wishtrace.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wishtrace.app.data.GiftDiscoveryGateway
import com.wishtrace.app.data.SeededGiftDiscoveryGateway
import com.wishtrace.app.domain.DiscoveryStage
import com.wishtrace.app.domain.GiftDiscoveryRequest
import com.wishtrace.app.domain.SourceMode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface DiscoveryUiState {
    data object Idle : DiscoveryUiState

    data class Running(
        val activeStage: DiscoveryStage,
        val completedStages: List<DiscoveryStage>,
    ) : DiscoveryUiState

    data class ReadyForRanking(
        val eligibleCandidateIds: List<String>,
        val sourceMode: SourceMode,
    ) : DiscoveryUiState

    data object Cancelled : DiscoveryUiState

    data class Error(val message: String) : DiscoveryUiState
}

class DiscoveryViewModel(
    private val gateway: GiftDiscoveryGateway = SeededGiftDiscoveryGateway(),
) : ViewModel() {
    private val mutableState = MutableStateFlow<DiscoveryUiState>(DiscoveryUiState.Idle)
    val state: StateFlow<DiscoveryUiState> = mutableState.asStateFlow()

    private var activeJob: Job? = null

    fun start(request: GiftDiscoveryRequest) {
        if (activeJob?.isActive == true) return

        val newJob = viewModelScope.launch {
            val completedStages = mutableListOf<DiscoveryStage>()
            var previousStage: DiscoveryStage? = null

            try {
                val preparation = gateway.prepareCandidates(request) { stage ->
                    previousStage?.let(completedStages::add)
                    mutableState.value = DiscoveryUiState.Running(
                        activeStage = stage,
                        completedStages = completedStages.toList(),
                    )
                    previousStage = stage
                }
                mutableState.value = DiscoveryUiState.ReadyForRanking(
                    eligibleCandidateIds = preparation.eligibleCandidateIds,
                    sourceMode = preparation.sourceMode,
                )
            } catch (_: CancellationException) {
                mutableState.value = DiscoveryUiState.Cancelled
            } catch (_: Exception) {
                mutableState.value = DiscoveryUiState.Error(
                    message = "The controlled catalog could not be checked. No ranking or purchase was started.",
                )
            }
        }

        activeJob = newJob
        newJob.invokeOnCompletion {
            if (activeJob === newJob) {
                activeJob = null
            }
        }
    }

    fun cancel() {
        val runningJob = activeJob
        if (runningJob?.isActive != true) return

        mutableState.value = DiscoveryUiState.Cancelled
        runningJob.cancel()
    }
}
