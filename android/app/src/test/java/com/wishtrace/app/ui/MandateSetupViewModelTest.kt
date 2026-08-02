package com.wishtrace.app.ui

import com.wishtrace.app.data.MandateGateway
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
        assertEquals(1, gateway.executeKeys.distinct().size)
        assertEquals(MandateSetupStep.PROOF_DECLINED, viewModel.state.value.step)
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

    private class FakeMandateGateway : MandateGateway {
        var current = details(MandateStatus.ACTIVE)
        var executeCalls = 0
        var lastBilling: BillingContact? = null
        val executeKeys = mutableListOf<String>()

        override suspend fun fetch(occasionId: String): MandateDetails = current

        override suspend fun setup(
            occasionId: String,
            candidateId: String,
        ): MandateDetails = current

        override suspend fun refresh(occasionId: String): MandateDetails = current

        override suspend fun execute(
            occasionId: String,
            billing: BillingContact,
            idempotencyKey: String,
        ): MandateDetails {
            executeCalls += 1
            lastBilling = billing
            executeKeys += idempotencyKey
            delay(10)
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
            createdAt = Instant.parse("2026-08-02T00:00:00Z"),
            updatedAt = Instant.parse("2026-08-02T00:01:00Z"),
        )
    }
}
