package com.wishtrace.app.ui

import com.wishtrace.app.data.PurchaseFlowGateway
import com.wishtrace.app.domain.ApprovalSession
import com.wishtrace.app.domain.BillingContact
import com.wishtrace.app.domain.Money
import com.wishtrace.app.domain.PurchaseIntentDetails
import com.wishtrace.app.domain.TransactionState
import com.wishtrace.app.domain.VerifiedResult
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CheckoutViewModelTest {
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
    fun repeatedStartCreatesOnePurchaseIntent() = runTest(dispatcher) {
        val gateway = FakeGateway()
        val viewModel = CheckoutViewModel(gateway)

        viewModel.start("candidate")
        viewModel.start("candidate")
        advanceUntilIdle()

        assertEquals(1, gateway.createCalls)
        assertEquals(CheckoutStep.REVIEW, viewModel.state.value.step)
    }

    @Test
    fun repeatedQuoteTapUsesOneOperationAndOneStableKey() = runTest(dispatcher) {
        val gateway = FakeGateway()
        val viewModel = CheckoutViewModel(gateway)
        viewModel.start("candidate")
        advanceUntilIdle()
        viewModel.updateBilling {
            BillingForm(
                firstName = "Test",
                lastName = "Buyer",
                addressLine1 = "1 Main Street",
                city = "Seattle",
                region = "WA",
                postalCode = "98101",
            )
        }

        viewModel.requestQuote("buyer@example.com")
        viewModel.requestQuote("buyer@example.com")
        advanceUntilIdle()

        assertEquals(1, gateway.quoteCalls)
        assertEquals(1, gateway.quoteKeys.distinct().size)
        assertEquals(CheckoutStep.READY_FOR_APPROVAL, viewModel.state.value.step)
    }

    @Test
    fun hostedApprovalUrlIsAnExplicitOneShotEvent() = runTest(dispatcher) {
        val gateway = FakeGateway()
        val viewModel = CheckoutViewModel(gateway)
        viewModel.start("candidate")
        advanceUntilIdle()
        viewModel.requestQuoteAfterReadyForTest()
        advanceUntilIdle()
        viewModel.createApprovalSession()
        advanceUntilIdle()

        assertTrue(viewModel.state.value.approvalUrl?.startsWith("https://") == true)
        viewModel.consumeApprovalUrl()
        assertNull(viewModel.state.value.approvalUrl)
    }

    @Test
    fun authoritativeDeclineLoadsAuthorizationResult() = runTest(dispatcher) {
        val gateway = FakeGateway().apply {
            current = details(TransactionState.AWAITING_USER, withSession = true)
        }
        val viewModel = CheckoutViewModel(gateway)

        viewModel.resumeFromReturn("intent")
        advanceUntilIdle()

        assertEquals(CheckoutStep.AUTHORIZATION_DECLINED, viewModel.state.value.step)
        assertTrue(viewModel.state.value.result is VerifiedResult.AuthorizationDeclined)
        assertEquals(1, gateway.reconcileCalls)
        assertEquals(1, gateway.receiptCalls)
    }

    private fun CheckoutViewModel.requestQuoteAfterReadyForTest() {
        val billing = BillingForm(
            firstName = "Test",
            lastName = "Buyer",
            addressLine1 = "1 Main Street",
            city = "Seattle",
            region = "WA",
            postalCode = "98101",
        )
        updateBilling { billing }
        requestQuote("buyer@example.com")
    }

    private class FakeGateway : PurchaseFlowGateway {
        var current = details(TransactionState.DRAFT)
        var createCalls = 0
        var quoteCalls = 0
        var reconcileCalls = 0
        var receiptCalls = 0
        val quoteKeys = mutableListOf<String>()

        override suspend fun createIntent(candidateId: String): PurchaseIntentDetails {
            createCalls += 1
            delay(10)
            current = details(TransactionState.DRAFT)
            return current
        }

        override suspend fun getIntent(purchaseIntentId: String): PurchaseIntentDetails = current

        override suspend fun quote(
            purchaseIntentId: String,
            billing: BillingContact,
            idempotencyKey: String,
        ): PurchaseIntentDetails {
            quoteCalls += 1
            quoteKeys += idempotencyKey
            delay(10)
            current = details(TransactionState.READY_FOR_APPROVAL)
            return current
        }

        override suspend fun createApprovalSession(
            purchaseIntentId: String,
            idempotencyKey: String,
        ): PurchaseIntentDetails {
            current = details(TransactionState.AWAITING_USER, withSession = true)
            return current
        }

        override suspend fun reconcile(purchaseIntentId: String): PurchaseIntentDetails {
            reconcileCalls += 1
            current = details(TransactionState.DECLINED, withSession = true)
            return current
        }

        override suspend fun getVerifiedResult(purchaseIntentId: String): VerifiedResult {
            receiptCalls += 1
            return VerifiedResult.AuthorizationDeclined(
                purchaseIntentId = "intent",
                merchantName = "Merchant",
                title = "Observed gift",
                amount = Money(500, "USD"),
                message = "Merchant decline recorded; Prava result confirmed.",
            )
        }

        override suspend fun saveMessage(purchaseIntentId: String, text: String) = Unit
    }

    private companion object {
        fun details(
            state: TransactionState,
            withSession: Boolean = false,
        ) = PurchaseIntentDetails(
            id = "intent",
            recipientId = "recipient",
            occasionId = "occasion",
            candidateId = "candidate",
            state = state,
            merchantName = "Merchant",
            merchantUrl = "https://example.com",
            title = "Observed gift",
            variantTitle = "$5",
            itemPrice = Money(500, "USD"),
            approvedTotal = Money(500, "USD").takeIf {
                state != TransactionState.DRAFT
            },
            deliverySummary = null,
            quoteExpiresAt = Instant.parse("2026-08-01T01:00:00Z"),
            approvalSession = if (withSession) {
                ApprovalSession(
                    id = "session",
                    hostedUrl = "https://sandbox.collect.prava.space/checkout?session=test",
                    expiresAt = Instant.parse("2026-08-01T01:00:00Z"),
                )
            } else {
                null
            },
            providerStatus = if (state == TransactionState.DECLINED) "failed" else null,
            merchantOrderId = null,
            updatedAt = Instant.parse("2026-08-01T00:00:00Z"),
        )
    }
}
