package com.wishtrace.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wishtrace.app.data.PurchaseFlowGateway
import com.wishtrace.app.data.WishTraceApiException
import com.wishtrace.app.domain.BillingContact
import com.wishtrace.app.domain.PurchaseIntentDetails
import com.wishtrace.app.domain.TransactionState
import com.wishtrace.app.domain.VerifiedResult
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class CheckoutStep {
    IDLE,
    LOADING,
    REVIEW,
    QUOTING,
    READY_FOR_APPROVAL,
    CREATING_APPROVAL,
    AWAITING_APPROVAL,
    RECONCILING,
    AUTHORIZATION_DECLINED,
    ORDER_SUCCEEDED,
    CANCELLED,
    EXPIRED,
    FAILED,
    UNKNOWN,
}

data class BillingForm(
    val firstName: String = "",
    val lastName: String = "",
    val addressLine1: String = "",
    val addressLine2: String = "",
    val city: String = "",
    val region: String = "",
    val postalCode: String = "",
    val countryCode: String = "US",
    val phone: String = "",
)

data class CheckoutUiState(
    val step: CheckoutStep = CheckoutStep.IDLE,
    val intent: PurchaseIntentDetails? = null,
    val billing: BillingForm = BillingForm(),
    val approvalUrl: String? = null,
    val result: VerifiedResult? = null,
    val error: String? = null,
    val messageText: String = "",
    val messageSaved: Boolean = false,
) {
    val busy: Boolean
        get() = step in setOf(
            CheckoutStep.LOADING,
            CheckoutStep.QUOTING,
            CheckoutStep.CREATING_APPROVAL,
            CheckoutStep.RECONCILING,
        )
}

class CheckoutViewModel(
    private val gateway: PurchaseFlowGateway,
) : ViewModel() {
    private val mutableState = MutableStateFlow(CheckoutUiState())
    val state: StateFlow<CheckoutUiState> = mutableState.asStateFlow()

    private var quoteKey: String? = null
    private var approvalKey: String? = null
    private var openedApprovalSessionId: String? = null
    private var savingMessage = false

    fun start(candidateId: String) {
        if (mutableState.value.busy || mutableState.value.intent?.candidateId == candidateId) return
        mutableState.update { it.copy(step = CheckoutStep.LOADING, error = null) }
        viewModelScope.launch {
            runCatching { gateway.createIntent(candidateId) }
                .onSuccess(::applyIntent)
                .onFailure(::showError)
        }
    }

    fun updateBilling(update: (BillingForm) -> BillingForm) {
        if (mutableState.value.busy) return
        quoteKey = null
        mutableState.update {
            it.copy(billing = update(it.billing), error = null)
        }
    }

    /**
     * Fills the form with a clearly synthetic US test address for the Prava sandbox
     * expected-failure proof. This is not a real address and only reaches sandbox builds
     * where [R.bool.wishtrace_sandbox_tools] is enabled. The verified Google email is left
     * untouched and the card is still supplied by Prava, never typed here.
     */
    fun useSandboxBillingAddress() {
        if (mutableState.value.busy) return
        quoteKey = null
        mutableState.update {
            it.copy(billing = SANDBOX_TEST_BILLING, error = null)
        }
    }

    fun requestQuote(verifiedEmail: String?) {
        val current = mutableState.value
        val intent = current.intent ?: return
        if (current.busy) return
        val contact = runCatching { current.billing.toContact(verifiedEmail) }
            .getOrElse { validationError ->
                mutableState.update { state ->
                    state.copy(
                        error = validationError.message
                            ?: "Complete the required billing fields.",
                    )
                }
                return
            }
        val stableKey = quoteKey ?: "quote-${UUID.randomUUID()}".also { quoteKey = it }
        mutableState.update { it.copy(step = CheckoutStep.QUOTING, error = null) }
        viewModelScope.launch {
            runCatching { gateway.quote(intent.id, contact, stableKey) }
                .onSuccess(::applyIntent)
                .onFailure(::showError)
        }
    }

    fun createApprovalSession() {
        val current = mutableState.value
        val intent = current.intent ?: return
        if (current.busy) return
        if (intent.state == TransactionState.AWAITING_USER && intent.approvalSession != null) {
            openedApprovalSessionId = null
            mutableState.update {
                it.copy(
                    step = CheckoutStep.AWAITING_APPROVAL,
                    approvalUrl = intent.approvalSession.hostedUrl,
                    error = null,
                )
            }
            return
        }
        if (intent.state != TransactionState.READY_FOR_APPROVAL) return
        val stableKey = approvalKey ?: "approval-${UUID.randomUUID()}".also {
            approvalKey = it
        }
        mutableState.update { it.copy(step = CheckoutStep.CREATING_APPROVAL, error = null) }
        viewModelScope.launch {
            runCatching { gateway.createApprovalSession(intent.id, stableKey) }
                .onSuccess(::applyIntent)
                .onFailure(::showError)
        }
    }

    fun consumeApprovalUrl() {
        openedApprovalSessionId = mutableState.value.intent?.approvalSession?.id
        mutableState.update { it.copy(approvalUrl = null) }
    }

    fun approvalLaunchFailed() {
        mutableState.update {
            it.copy(
                step = CheckoutStep.READY_FOR_APPROVAL,
                approvalUrl = null,
                error = "Prava could not open securely. Try again.",
            )
        }
    }

    fun resumeFromReturn(purchaseIntentId: String) {
        if (purchaseIntentId.isBlank() || mutableState.value.busy) return
        mutableState.update { it.copy(step = CheckoutStep.RECONCILING, error = null) }
        viewModelScope.launch {
            runCatching {
                val existing = if (mutableState.value.intent?.id == purchaseIntentId) {
                    mutableState.value.intent
                } else {
                    gateway.getIntent(purchaseIntentId)
                }
                require(existing != null)
                openedApprovalSessionId = existing.approvalSession?.id
                gateway.reconcile(purchaseIntentId)
            }.onSuccess(::applyIntent)
                .onFailure(::showError)
        }
    }

    fun refresh() {
        val id = mutableState.value.intent?.id ?: return
        resumeFromReturn(id)
    }

    fun updateMessage(value: String) {
        mutableState.update {
            it.copy(messageText = value.take(500), messageSaved = false, error = null)
        }
    }

    fun saveMessage() {
        val current = mutableState.value
        val id = current.intent?.id ?: return
        val text = current.messageText.trim()
        if (text.isBlank() || current.busy || savingMessage) {
            mutableState.update { it.copy(error = "Write a note before saving it.") }
            return
        }
        savingMessage = true
        viewModelScope.launch {
            runCatching { gateway.saveMessage(id, text) }
                .onSuccess {
                    mutableState.update { it.copy(messageSaved = true, error = null) }
                }
                .onFailure(::showError)
            savingMessage = false
        }
    }

    private fun applyIntent(intent: PurchaseIntentDetails) {
        val baseStep = when (intent.state) {
            TransactionState.DRAFT,
            TransactionState.VALIDATING,
            -> CheckoutStep.REVIEW

            TransactionState.QUOTED,
            TransactionState.READY_FOR_APPROVAL,
            -> CheckoutStep.READY_FOR_APPROVAL

            TransactionState.SESSION_CREATING -> CheckoutStep.CREATING_APPROVAL
            TransactionState.AWAITING_USER,
            TransactionState.CREDENTIALS_READY,
            TransactionState.CHECKOUT_IN_PROGRESS,
            TransactionState.ORDER_VERIFIED,
            -> CheckoutStep.AWAITING_APPROVAL

            TransactionState.RECONCILING -> CheckoutStep.RECONCILING
            TransactionState.CANCELLED -> CheckoutStep.CANCELLED
            TransactionState.EXPIRED -> CheckoutStep.EXPIRED
            TransactionState.FAILED -> CheckoutStep.FAILED
            TransactionState.UNKNOWN -> CheckoutStep.UNKNOWN
            TransactionState.DECLINED -> CheckoutStep.AUTHORIZATION_DECLINED
            TransactionState.SUCCEEDED -> CheckoutStep.ORDER_SUCCEEDED
        }
        mutableState.update {
            it.copy(
                step = baseStep,
                intent = intent,
                approvalUrl = intent.approvalSession?.hostedUrl
                    ?.takeIf {
                        intent.state == TransactionState.AWAITING_USER &&
                            intent.approvalSession.id != openedApprovalSessionId
                    },
                error = null,
            )
        }
        if (intent.state == TransactionState.DECLINED || intent.state == TransactionState.SUCCEEDED) {
            viewModelScope.launch {
                runCatching { gateway.getVerifiedResult(intent.id) }
                    .onSuccess { result -> mutableState.update { it.copy(result = result) } }
                    .onFailure(::showError)
            }
        }
    }

    private fun showError(error: Throwable) {
        val safeMessage = (error as? WishTraceApiException)?.message
            ?: "WishTrace could not complete that step. Try again."
        val fallback = mutableState.value.intent?.state?.toStep() ?: CheckoutStep.FAILED
        mutableState.update { it.copy(step = fallback, error = safeMessage) }
    }
}

private val SANDBOX_TEST_BILLING = BillingForm(
    firstName = "Test",
    lastName = "Gifter",
    addressLine1 = "123 Test Street",
    addressLine2 = "",
    city = "Seattle",
    region = "WA",
    postalCode = "98101",
    countryCode = "US",
    phone = "",
)

private fun BillingForm.toContact(email: String?): BillingContact = BillingContact(
    email = email?.takeIf(String::isNotBlank)
        ?: throw IllegalArgumentException("A verified sign-in email is required."),
    firstName = firstName.trim(),
    lastName = lastName.trim(),
    addressLine1 = addressLine1.trim(),
    addressLine2 = addressLine2.trim().takeIf(String::isNotEmpty),
    city = city.trim(),
    region = region.trim().takeIf(String::isNotEmpty),
    postalCode = postalCode.trim(),
    countryCode = countryCode.trim().uppercase(),
    phone = phone.trim().takeIf(String::isNotEmpty),
)

private fun TransactionState?.toStep(): CheckoutStep = when (this) {
    TransactionState.READY_FOR_APPROVAL,
    TransactionState.QUOTED,
    -> CheckoutStep.READY_FOR_APPROVAL

    TransactionState.AWAITING_USER,
    TransactionState.CREDENTIALS_READY,
    TransactionState.CHECKOUT_IN_PROGRESS,
    TransactionState.ORDER_VERIFIED,
    -> CheckoutStep.AWAITING_APPROVAL

    TransactionState.DECLINED -> CheckoutStep.AUTHORIZATION_DECLINED
    TransactionState.SUCCEEDED -> CheckoutStep.ORDER_SUCCEEDED
    TransactionState.CANCELLED -> CheckoutStep.CANCELLED
    TransactionState.EXPIRED -> CheckoutStep.EXPIRED
    TransactionState.UNKNOWN -> CheckoutStep.UNKNOWN
    TransactionState.FAILED -> CheckoutStep.FAILED
    else -> CheckoutStep.REVIEW
}
