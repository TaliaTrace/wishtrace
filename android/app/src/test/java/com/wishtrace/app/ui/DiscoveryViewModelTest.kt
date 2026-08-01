package com.wishtrace.app.ui

import com.wishtrace.app.data.GiftDiscoveryGateway
import com.wishtrace.app.domain.DiscoveryPreparation
import com.wishtrace.app.domain.DiscoveryStage
import com.wishtrace.app.domain.GiftDiscoveryRequest
import com.wishtrace.app.domain.Money
import com.wishtrace.app.domain.SourceMode
import com.wishtrace.app.domain.AvailabilityState
import com.wishtrace.app.domain.CandidateRationale
import com.wishtrace.app.domain.ProductCandidate
import com.wishtrace.app.domain.RankedDecision
import com.wishtrace.app.domain.RankingUncertainty
import java.io.IOException
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DiscoveryViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val request = GiftDiscoveryRequest(
        recipientId = "recipient_sophie",
        occasionId = "occasion_sophie_birthday",
        budget = Money(minorUnits = 6_000, currencyCode = "USD"),
    )

    @Before
    fun setMainDispatcher() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun resetMainDispatcher() {
        Dispatchers.resetMain()
    }

    @Test
    fun repeatedStartWhileActiveRunsOnlyOnce() = runTest(dispatcher) {
        val gateway = CountingGateway()
        val viewModel = DiscoveryViewModel(gateway)

        viewModel.start(request)
        viewModel.start(request)
        advanceUntilIdle()

        assertEquals(1, gateway.invocationCount)
        assertTrue(viewModel.state.value is DiscoveryUiState.ReadyForRanking)
    }

    @Test
    fun cancelStopsTheActiveRunWithoutCreatingAResult() = runTest(dispatcher) {
        val gateway = SuspendedGateway()
        val viewModel = DiscoveryViewModel(gateway)

        viewModel.start(request)
        runCurrent()
        viewModel.cancel()
        runCurrent()

        assertEquals(DiscoveryUiState.Cancelled, viewModel.state.value)
    }

    @Test
    fun gatewayFailureBecomesRecoverableError() = runTest(dispatcher) {
        val viewModel = DiscoveryViewModel(FailingGateway())

        viewModel.start(request)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertTrue(state is DiscoveryUiState.Error)
        assertTrue((state as DiscoveryUiState.Error).message.contains("No purchase"))
    }

    private inner class CountingGateway : GiftDiscoveryGateway {
        var invocationCount: Int = 0

        override suspend fun prepareCandidates(
            request: GiftDiscoveryRequest,
            onStage: suspend (DiscoveryStage) -> Unit,
        ): DiscoveryPreparation {
            invocationCount += 1
            DiscoveryStage.entries.forEach { stage ->
                onStage(stage)
                delay(10)
            }
            return DiscoveryPreparation(
                discoveryId = "discovery_alpha",
                candidates = listOf(candidate()),
                decision = decision(),
                eligibleCandidateIds = listOf("candidate_alpha"),
                sourceMode = SourceMode.LIVE,
            )
        }
    }

    private class SuspendedGateway : GiftDiscoveryGateway {
        override suspend fun prepareCandidates(
            request: GiftDiscoveryRequest,
            onStage: suspend (DiscoveryStage) -> Unit,
        ): DiscoveryPreparation {
            onStage(DiscoveryStage.CHECKING_CATALOG)
            delay(Long.MAX_VALUE)
            error("Cancellation should prevent this line.")
        }
    }

    private class FailingGateway : GiftDiscoveryGateway {
        override suspend fun prepareCandidates(
            request: GiftDiscoveryRequest,
            onStage: suspend (DiscoveryStage) -> Unit,
        ): DiscoveryPreparation = throw IOException("Disconnected")
    }

    private fun candidate() = ProductCandidate(
        id = "candidate_alpha",
        merchantId = "merchant_alpha",
        merchantName = "Merchant",
        title = "Observed gift",
        currentPrice = Money(500, "USD"),
        productUrl = "https://example.com/product",
        checkoutReference = "variant_alpha",
        availability = AvailabilityState.AVAILABLE,
        requiredVariant = null,
        selectedVariant = "$5",
        supportedDeliveryFact = null,
        arrivesBy = null,
        sourceTimestamp = Instant.parse("2026-08-01T00:00:00Z"),
        sourceMode = SourceMode.LIVE,
    )

    private fun decision() = RankedDecision(
        selectedCandidateId = "candidate_alpha",
        alternativeCandidateIds = emptyList(),
        rationales = listOf(
            CandidateRationale(
                candidateId = "candidate_alpha",
                evidenceIds = listOf("interest_games"),
                reason = "Matches the saved interest.",
            ),
        ),
        rejections = emptyList(),
        uncertainty = RankingUncertainty.LOW,
        modelRequestId = null,
        promptVersion = "test-v1",
    )
}
