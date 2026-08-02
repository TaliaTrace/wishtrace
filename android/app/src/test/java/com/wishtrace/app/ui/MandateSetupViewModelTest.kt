package com.wishtrace.app.ui

import com.wishtrace.app.data.MandateGateway
import com.wishtrace.app.data.WishTraceApiException
import com.wishtrace.app.domain.BillingContact
import com.wishtrace.app.domain.MandateDetails
import com.wishtrace.app.domain.MandateMerchantOutcome
import com.wishtrace.app.domain.MandateStatus
import com.wishtrace.app.domain.MandateVisaConfirmation
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MandateSetupViewModelTest {
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
    fun sandboxProofUsesVerifiedEmailAndRejectsDuplicateTap() = runTest(dispatcher) {
        val gateway = FakeMandateGateway()
        val viewModel = MandateSetupViewModel(gateway)

        viewModel.start("occasion")
        advanceUntilIdle()
        viewModel.executeSandboxProof("owner@example.com")
        viewModel.executeSandboxProof("owner@example.com")
        advanceUntilIdle()

        assertEquals(1, gateway.executeCalls)
        assertEquals("owner@example.com", gateway.lastBilling?.email)
        assertEquals("OR", gateway.lastBilling?.region)
        assertEquals("97205", gateway.lastBilling?.postalCode)
        assertEquals(1, gateway.executeKeys.distinct().size)
        assertEquals("mandate-proof-mandate", gateway.executeKeys.single())
        assertEquals(MandateSetupStep.PROOF_DECLINED, viewModel.state.value.step)
    }

    @Test
    fun openingExistingMandateAlwaysRefreshesProviderState() = runTest(dispatcher) {
        val gateway = FakeMandateGateway().apply {
            current = details(MandateStatus.AWAITING_APPROVAL)
            refreshResult = details(MandateStatus.ACTIVE)
        }
        val viewModel = MandateSetupViewModel(gateway)

        viewModel.start("occasion")
        advanceUntilIdle()
        assertEquals(MandateSetupStep.AWAITING_APPROVAL, viewModel.state.value.step)

        viewModel.openExisting("occasion")
        advanceUntilIdle()

        assertEquals(1, gateway.refreshCalls)
        assertEquals(MandateSetupStep.ACTIVE, viewModel.state.value.step)
    }

    @Test
    fun executeFailureReconcilesServerStateAndCannotLookActive() = runTest(dispatcher) {
        val gateway = FakeMandateGateway().apply {
            executeFailureState = details(
                status = MandateStatus.ACTIVE,
                lastChargeState = "FAILED",
                lastChargeAmountMinor = 1_101,
                lastChargeFailureCode = "MERCHANT_TOTAL_EXCEEDS_MANDATE",
            )
            executeError = WishTraceApiException(
                message = "The live total is \$11.01, above your \$10.00 autopilot cap.",
                code = "MERCHANT_TOTAL_EXCEEDS_MANDATE",
                recoverable = true,
            )
        }
        val viewModel = MandateSetupViewModel(gateway)

        viewModel.start("occasion")
        advanceUntilIdle()
        viewModel.executeSandboxProof("owner@example.com")
        advanceUntilIdle()

        assertEquals(1, gateway.executeCalls)
        assertEquals(MandateSetupStep.PROOF_BLOCKED, viewModel.state.value.step)
        assertEquals(
            "MERCHANT_TOTAL_EXCEEDS_MANDATE",
            viewModel.state.value.mandate?.lastChargeFailureCode,
        )
        assertTrue(viewModel.state.value.error?.contains("\$11.01") == true)
    }

    @Test
    fun sandboxProofRequiresVerifiedEmail() = runTest(dispatcher) {
        val gateway = FakeMandateGateway()
        val viewModel = MandateSetupViewModel(gateway)

        viewModel.start("occasion")
        advanceUntilIdle()
        viewModel.executeSandboxProof(null)
        advanceUntilIdle()

        assertEquals(0, gateway.executeCalls)
        assertEquals(MandateSetupStep.ACTIVE, viewModel.state.value.step)
        assertNotNull(viewModel.state.value.error)
    }

    @Test
    fun verifiedMerchantOrderIsNotConfusedWithMerelyArmed() = runTest(dispatcher) {
        val gateway = FakeMandateGateway().apply {
            current = details(
                status = MandateStatus.CONSUMED,
                merchantOutcome = MandateMerchantOutcome.ORDER_VERIFIED,
                orderId = "order-safe-id",
                lastChargeState = "SUCCEEDED",
            )
        }
        val viewModel = MandateSetupViewModel(gateway)

        viewModel.start("occasion")
        advanceUntilIdle()

        assertEquals(MandateSetupStep.PROOF_COMPLETE, viewModel.state.value.step)
        assertTrue(viewModel.state.value.mandate?.merchantOrderId == "order-safe-id")
    }

    @Test
    fun consumedSandboxDeclineIsNotPresentedAsOrderSuccess() = runTest(dispatcher) {
        val gateway = FakeMandateGateway().apply {
            current = details(
                status = MandateStatus.CONSUMED,
                merchantOutcome = MandateMerchantOutcome.DECLINED,
                lastChargeState = "DECLINED",
            )
        }
        val viewModel = MandateSetupViewModel(gateway)

        viewModel.start("occasion")
        advanceUntilIdle()

        assertEquals(MandateSetupStep.PROOF_DECLINED, viewModel.state.value.step)
    }

    @Test
    fun failedCardIssueOffersOneExplicitRetryWithoutNewApproval() = runTest(dispatcher) {
        val gateway = FakeMandateGateway().apply {
            current = details(
                status = MandateStatus.DECLINED,
                lastChargeState = "DECLINED",
                lastChargeFailureCode = "FETCH_AGENTIC_CREDS_ERROR",
                mintRetryAvailable = true,
            )
        }
        val viewModel = MandateSetupViewModel(gateway)

        viewModel.start("occasion")
        advanceUntilIdle()
        viewModel.retryCardIssue("owner@example.com")
        viewModel.retryCardIssue("owner@example.com")
        advanceUntilIdle()

        assertEquals(1, gateway.executeCalls)
        assertEquals(0, gateway.setupCalls)
        assertEquals("mandate-proof-mandate-mint-retry-1", gateway.executeKeys.single())
    }

    @Test
    fun activeProviderCannotHideUnknownMerchantAttempt() = runTest(dispatcher) {
        val gateway = FakeMandateGateway().apply {
            current = details(
                status = MandateStatus.ACTIVE,
                merchantOutcome = MandateMerchantOutcome.UNKNOWN,
                lastChargeState = "UNKNOWN",
            )
        }
        val viewModel = MandateSetupViewModel(gateway)

        viewModel.start("occasion")
        advanceUntilIdle()

        assertEquals(MandateSetupStep.UNKNOWN, viewModel.state.value.step)
    }

    @Test
    fun setupConflictAutomaticallyRefreshesExistingMandate() = runTest(dispatcher) {
        val unknown = details(
            status = MandateStatus.UNKNOWN,
            merchantOutcome = MandateMerchantOutcome.UNKNOWN,
            lastChargeState = "UNKNOWN",
        )
        val gateway = FakeMandateGateway().apply {
            current = unknown
            refreshResult = unknown
            setupError = WishTraceApiException(
                message = "WishTrace found an existing approval.",
                code = "MANDATE_ALREADY_EXISTS",
                recoverable = true,
            )
        }
        val viewModel = MandateSetupViewModel(gateway)

        viewModel.start("occasion")
        advanceUntilIdle()
        viewModel.prepareSelection("occasion")
        viewModel.arm("candidate-new")
        advanceUntilIdle()

        assertEquals(1, gateway.refreshCalls)
        assertEquals(MandateSetupStep.UNKNOWN, viewModel.state.value.step)
        assertEquals(null, viewModel.state.value.error)
    }

    @Test
    fun explicitUnknownRecoverySendsExactMandateBeingReplaced() = runTest(dispatcher) {
        val gateway = FakeMandateGateway().apply {
            current = details(
                status = MandateStatus.UNKNOWN,
                merchantOutcome = MandateMerchantOutcome.UNKNOWN,
                lastChargeState = "UNKNOWN",
            )
            setupResult = details(MandateStatus.AWAITING_APPROVAL)
        }
        val viewModel = MandateSetupViewModel(gateway)

        viewModel.start("occasion")
        advanceUntilIdle()
        viewModel.prepareUnknownReplacement()
        viewModel.prepareSelection("occasion")
        viewModel.arm("candidate-new")
        advanceUntilIdle()

        assertEquals("mandate", gateway.lastReplacementMandateId)
        assertEquals(MandateSetupStep.AWAITING_APPROVAL, viewModel.state.value.step)
    }

    @Test
    fun passkeyFailureAllowsOneExplicitFreshApproval() = runTest(dispatcher) {
        val gateway = FakeMandateGateway().apply {
            current = details(
                status = MandateStatus.FAILED,
                setupFailureCode = "AUTH_FAILED",
            )
            setupResult = details(MandateStatus.AWAITING_APPROVAL)
        }
        val viewModel = MandateSetupViewModel(gateway)

        viewModel.start("occasion")
        advanceUntilIdle()
        assertTrue(viewModel.state.value.canRetryApproval)

        viewModel.retryApproval("candidate-drawful")
        advanceUntilIdle()

        assertEquals(1, gateway.setupCalls)
        assertEquals("candidate-drawful", gateway.lastCandidateId)
        assertEquals(MandateSetupStep.AWAITING_APPROVAL, viewModel.state.value.step)
    }

    @Test
    fun provisioningFailureDoesNotOfferAutomaticRecovery() = runTest(dispatcher) {
        val gateway = FakeMandateGateway().apply {
            current = details(
                status = MandateStatus.FAILED,
                setupFailureCode = "PROVISION_ERROR",
            )
        }
        val viewModel = MandateSetupViewModel(gateway)

        viewModel.start("occasion")
        advanceUntilIdle()
        viewModel.retryApproval("candidate-drawful")
        advanceUntilIdle()

        assertTrue(!viewModel.state.value.canRetryApproval)
        assertEquals(0, gateway.setupCalls)
    }

    @Test
    fun exhaustedCredentialMintOffersFreshSandboxCardPathOnlyBeforeMerchant() =
        runTest(dispatcher) {
            val gateway = FakeMandateGateway().apply {
                current = details(
                    status = MandateStatus.DECLINED,
                    lastChargeState = "DECLINED",
                    lastChargeFailureCode = "FETCH_AGENTIC_CREDS_ERROR",
                    mintRetryAvailable = false,
                )
            }
            val viewModel = MandateSetupViewModel(gateway)

            viewModel.start("occasion")
            advanceUntilIdle()

            assertEquals(MandateSetupStep.PROOF_DECLINED, viewModel.state.value.step)
            assertTrue(viewModel.state.value.canChooseAnotherSandboxCard)

            gateway.current = details(
                status = MandateStatus.CONSUMED,
                merchantOutcome = MandateMerchantOutcome.DECLINED,
                lastChargeState = "DECLINED",
                lastChargeFailureCode = "MERCHANT_PAYMENT_DECLINED",
            )
            val merchantViewModel = MandateSetupViewModel(gateway)
            merchantViewModel.start("occasion")
            advanceUntilIdle()

            assertTrue(!merchantViewModel.state.value.canChooseAnotherSandboxCard)
        }

    @Test
    fun choosingAgainRetiresActiveApprovalAndForcesFreshCardOnNextSetup() =
        runTest(dispatcher) {
            val gateway = FakeMandateGateway().apply {
                current = details(
                    status = MandateStatus.ACTIVE,
                    lastChargeState = "FAILED",
                    lastChargeFailureCode = "MERCHANT_QUOTE_FAILED",
                )
                setupResult = details(MandateStatus.AWAITING_APPROVAL)
            }
            val viewModel = MandateSetupViewModel(gateway)

            viewModel.start("occasion")
            advanceUntilIdle()
            viewModel.chooseAnotherGift()
            advanceUntilIdle()

            assertEquals(1, gateway.cancelCalls)
            assertTrue(viewModel.state.value.freshSelectionReady)

            viewModel.consumeFreshSelectionReady()
            viewModel.prepareSelection("occasion")
            viewModel.arm("candidate-fresh")
            advanceUntilIdle()

            assertEquals(1, gateway.setupCalls)
            assertTrue(gateway.lastRequireFreshCard)
            assertEquals(MandateSetupStep.AWAITING_APPROVAL, viewModel.state.value.step)
        }

    private class FakeMandateGateway : MandateGateway {
        var current = details(MandateStatus.ACTIVE)
        var setupResult: MandateDetails? = null
        var setupError: WishTraceApiException? = null
        var setupCalls = 0
        var refreshCalls = 0
        var refreshResult: MandateDetails? = null
        var lastCandidateId: String? = null
        var lastReplacementMandateId: String? = null
        var lastRequireFreshCard = false
        var cancelCalls = 0
        var executeCalls = 0
        var executeError: WishTraceApiException? = null
        var executeFailureState: MandateDetails? = null
        var lastBilling: BillingContact? = null
        val executeKeys = mutableListOf<String>()

        override suspend fun fetch(occasionId: String): MandateDetails = current

        override suspend fun setup(
            occasionId: String,
            candidateId: String,
            replaceUnknownMandateId: String?,
            requireFreshCard: Boolean,
        ): MandateDetails {
            setupCalls += 1
            lastCandidateId = candidateId
            lastReplacementMandateId = replaceUnknownMandateId
            lastRequireFreshCard = requireFreshCard
            setupError?.let { throw it }
            return setupResult ?: current
        }

        override suspend fun refresh(occasionId: String): MandateDetails {
            refreshCalls += 1
            current = refreshResult ?: current
            return current
        }

        override suspend fun cancel(occasionId: String): MandateDetails {
            cancelCalls += 1
            current = current.copy(status = MandateStatus.CANCELLED)
            return current
        }

        override suspend fun execute(
            occasionId: String,
            billing: BillingContact,
            idempotencyKey: String,
        ): MandateDetails {
            executeCalls += 1
            lastBilling = billing
            executeKeys += idempotencyKey
            delay(10)
            executeError?.let { error ->
                executeFailureState?.let { current = it }
                throw error
            }
            current = details(
                status = MandateStatus.DECLINED,
                merchantOutcome = MandateMerchantOutcome.DECLINED,
                lastChargeState = "DECLINED",
            )
            return current
        }
    }

    private companion object {
        fun details(
            status: MandateStatus,
            merchantOutcome: MandateMerchantOutcome? = null,
            orderId: String? = null,
            lastChargeState: String? = null,
            lastChargeAmountMinor: Int? = null,
            lastChargeFailureCode: String? = null,
            setupFailureCode: String? = null,
            mintRetryAvailable: Boolean = false,
        ) = MandateDetails(
            id = "mandate",
            recipientId = "recipient",
            occasionId = "occasion",
            status = status,
            approvedAmountMinor = 500,
            currency = "USD",
            recurringFrequency = "one_time",
            merchantScope = "listed",
            maxCharges = 1,
            chargesUsed = if (lastChargeState == null) 0 else 1,
            merchantName = "Jackbox Games",
            productTitle = "Jackbox Games Gift Card - $5 USD",
            itemPriceMinor = 500,
            approvalUrl = null,
            lastProviderStatus = status.wire.lowercase(),
            setupFailureCode = setupFailureCode,
            merchantOrderId = orderId,
            merchantOutcome = merchantOutcome,
            visaConfirmation = merchantOutcome?.let {
                if (it == MandateMerchantOutcome.ORDER_VERIFIED) {
                    MandateVisaConfirmation.SUCCESS
                } else {
                    MandateVisaConfirmation.FAILURE
                }
            },
            lastChargeState = lastChargeState,
            lastChargeAmountMinor = lastChargeAmountMinor,
            lastChargeFailureCode = lastChargeFailureCode,
            mintRetryAvailable = mintRetryAvailable,
            createdAt = Instant.parse("2026-08-02T00:00:00Z"),
            updatedAt = Instant.parse("2026-08-02T00:01:00Z"),
        )
    }
}
