package com.wishtrace.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wishtrace.app.data.MandateGateway
import com.wishtrace.app.data.WishTraceApiException
import com.wishtrace.app.domain.BillingContact
import com.wishtrace.app.domain.MandateDetails
import com.wishtrace.app.domain.MandateStatus
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** The mandate setup lifecycle, mirroring [CheckoutStep]'s approval/poll/reconcile. */
enum class MandateSetupStep {
    IDLE,
    LOADING,
    READY_TO_ARM,
    SETTING_UP,
    AWAITING_APPROVAL,
    REFRESHING,
    ACTIVE,
    PROOF_BLOCKED,
    EXECUTING,
    PROOF_COMPLETE,
    PROOF_DECLINED,
    DECLINED,
    CANCELLED,
    EXPIRED,
    FAILED,
    UNKNOWN,
}

data class MandateSetupUiState(
    val step: MandateSetupStep = MandateSetupStep.IDLE,
    val mandate: MandateDetails? = null,
    val approvalUrl: String? = null,
    val occasionId: String? = null,
    val error: String? = null,
) {
    val busy: Boolean
        get() = step in setOf(
            MandateSetupStep.LOADING,
            MandateSetupStep.SETTING_UP,
            MandateSetupStep.REFRESHING,
            MandateSetupStep.EXECUTING,
        )

    val canRetryApproval: Boolean
        get() = step == MandateSetupStep.FAILED && mandate?.let {
            it.lastChargeState == null && it.setupFailureCode in setOf(
                    "FIDO_START_FAILED",
                    "AUTH_FAILED",
                    "AUTH_CANCELLED",
                    "SESSION_EXPIRED",
                )
        } == true
}

class MandateSetupViewModel(
    private val gateway: MandateGateway,
) : ViewModel() {
    private val mutableState = MutableStateFlow(MandateSetupUiState())
    val state: StateFlow<MandateSetupUiState> = mutableState.asStateFlow()

    private var setupKey: String? = null
    private var executeKey: String? = null
    private var openedApprovalSessionId: String? = null
    private var replaceUnknownMandateId: String? = null

    /** Loads the occasion's current mandate, if any, and reconciles into a UI step. */
    fun start(occasionId: String) {
        val current = mutableState.value
        if (current.busy) return
        if (current.occasionId == occasionId && current.step != MandateSetupStep.IDLE) return
        if (current.occasionId != occasionId) replaceUnknownMandateId = null
        mutableState.update { it.copy(step = MandateSetupStep.LOADING, occasionId = occasionId, error = null) }
        viewModelScope.launch {
            runCatching { gateway.fetch(occasionId) }
                .onSuccess { mandate -> applyMandate(mandate) }
                .onFailure(::showError)
        }
    }

    /** Opens an existing occasion mandate and reconciles Prava before showing its state. */
    fun openExisting(occasionId: String) {
        replaceUnknownMandateId = null
        reconcile(occasionId)
    }

    /** Records the exact locked sandbox attempt before the user deliberately picks a new gift. */
    fun prepareUnknownReplacement() {
        val current = mutableState.value
        replaceUnknownMandateId = current.mandate?.id
            ?.takeIf { current.step == MandateSetupStep.UNKNOWN }
    }

    /** Begins an explicit new gift selection without replaying the prior attempt. */
    fun prepareSelection(occasionId: String) {
        if (mutableState.value.busy) return
        if (mutableState.value.occasionId != occasionId) replaceUnknownMandateId = null
        setupKey = null
        executeKey = null
        openedApprovalSessionId = null
        mutableState.value = MandateSetupUiState(
            step = MandateSetupStep.READY_TO_ARM,
            occasionId = occasionId,
        )
    }

    /**
     * Arms the autopilot: POST /setup with the chosen candidate. The response carries
     * the Prava approval URL, which the caller hands to a CustomTab.
     */
    fun arm(candidateId: String) {
        val current = mutableState.value
        val occasionId = current.occasionId ?: return
        if (current.busy) return
        if (current.mandate?.status?.requiresApproval == true) {
            openedApprovalSessionId = null
            mutableState.update {
                it.copy(
                    step = MandateSetupStep.AWAITING_APPROVAL,
                    approvalUrl = current.mandate.approvalUrl,
                    error = null,
                )
            }
            return
        }
        val stableKey = setupKey ?: "mandate-${UUID.randomUUID()}".also { setupKey = it }
        mutableState.update { it.copy(step = MandateSetupStep.SETTING_UP, error = null) }
        viewModelScope.launch {
            try {
                val mandate = gateway.setup(
                    occasionId = occasionId,
                    candidateId = candidateId,
                    replaceUnknownMandateId = replaceUnknownMandateId,
                )
                replaceUnknownMandateId = null
                applyMandate(mandate)
            } catch (error: Throwable) {
                if ((error as? WishTraceApiException)?.code == "MANDATE_ALREADY_EXISTS") {
                    replaceUnknownMandateId = null
                    mutableState.update {
                        it.copy(step = MandateSetupStep.REFRESHING, error = null)
                    }
                    runCatching { gateway.refresh(occasionId) }
                        .onSuccess(::applyMandate)
                        .onFailure(::showError)
                } else {
                    showError(error)
                }
            }
        }
    }

    fun consumeApprovalUrl() {
        openedApprovalSessionId = mutableState.value.mandate?.id
        mutableState.update { it.copy(approvalUrl = null) }
    }

    fun approvalLaunchFailed() {
        mutableState.update {
            it.copy(
                step = MandateSetupStep.READY_TO_ARM,
                approvalUrl = null,
                error = "Prava could not open securely. Try again.",
            )
        }
    }

    /** Starts one fresh hosted approval only after an explicit user action. */
    fun retryApproval(candidateId: String) {
        val occasionId = mutableState.value.occasionId ?: return
        if (mutableState.value.busy || !mutableState.value.canRetryApproval) return
        prepareSelection(occasionId)
        arm(candidateId)
    }

    /** The deep link came back — poll /refresh until ACTIVE or a terminal state. */
    fun resumeFromReturn(occasionId: String) {
        reconcile(occasionId)
    }

    private fun reconcile(occasionId: String) {
        if (occasionId.isBlank() || mutableState.value.busy) return
        // Persist the id up front so a cold-start deep link keeps the screen mounted
        // through the REFRESHING window, before applyMandate echoes it back.
        mutableState.update {
            it.copy(
                step = MandateSetupStep.REFRESHING,
                occasionId = occasionId,
                error = null,
            )
        }
        viewModelScope.launch {
            runCatching { gateway.refresh(occasionId) }
                .onSuccess(::applyMandate)
                .onFailure(::showError)
        }
    }

    fun refresh() {
        val occasionId = mutableState.value.occasionId ?: return
        resumeFromReturn(occasionId)
    }

    /**
     * Runs the organizer-required sandbox merchant proof against an active mandate.
     * The synthetic address is test-only; the verified account email remains authoritative,
     * and Prava's one-time credential never enters Android.
     */
    fun executeSandboxProof(verifiedEmail: String?) {
        val current = mutableState.value
        val occasionId = current.occasionId ?: return
        val email = verifiedEmail?.trim()?.takeIf(String::isNotEmpty)
        if (current.busy || current.mandate?.status != MandateStatus.ACTIVE) return
        if (email == null) {
            mutableState.update {
                it.copy(error = "Sign in again before running the sandbox merchant proof.")
            }
            return
        }
        val stableKey = executeKey
            ?: "mandate-proof-${current.mandate.id}".also { executeKey = it }
        mutableState.update {
            it.copy(step = MandateSetupStep.EXECUTING, error = null)
        }
        viewModelScope.launch {
            val result = runCatching {
                gateway.execute(
                    occasionId = occasionId,
                    billing = sandboxBilling(email),
                    idempotencyKey = stableKey,
                )
            }
            result.onSuccess(::applyMandate)
            result.onFailure { error ->
                val reconciled = runCatching { gateway.fetch(occasionId) }.getOrNull()
                if (reconciled == null) {
                    showError(error)
                } else {
                    applyMandate(reconciled)
                    mutableState.update {
                        it.copy(error = safeMessage(error))
                    }
                }
            }
        }
    }

    private fun applyMandate(mandate: MandateDetails?) {
        if (mandate == null) {
            mutableState.update {
                it.copy(step = MandateSetupStep.READY_TO_ARM, mandate = null, error = null)
            }
            return
        }
        val step = mandate.toStep()
        mutableState.update {
            it.copy(
                step = step,
                mandate = mandate,
                occasionId = mandate.occasionId,
                approvalUrl = mandate.approvalUrl?.takeIf {
                    mandate.status == MandateStatus.AWAITING_APPROVAL &&
                        mandate.id != openedApprovalSessionId
                },
                error = null,
            )
        }
    }

    private fun showError(error: Throwable) {
        val fallback = mutableState.value.mandate?.toStep() ?: MandateSetupStep.FAILED
        mutableState.update { it.copy(step = fallback, error = safeMessage(error)) }
    }
}

private fun safeMessage(error: Throwable): String =
    (error as? WishTraceApiException)?.message
        ?: "WishTrace could not complete that step. Try again."

private fun MandateDetails.toStep(): MandateSetupStep = when (status) {
    MandateStatus.AWAITING_APPROVAL,
    MandateStatus.SETUP_CREATING,
    -> MandateSetupStep.AWAITING_APPROVAL

    MandateStatus.ACTIVE -> when (lastChargeState) {
        null -> MandateSetupStep.ACTIVE
        "FAILED" -> MandateSetupStep.PROOF_BLOCKED
        "UNKNOWN" -> MandateSetupStep.UNKNOWN
        "DECLINED" -> MandateSetupStep.PROOF_DECLINED
        "SUCCEEDED" -> MandateSetupStep.PROOF_COMPLETE
        "CHARGING", "CHECKOUT_IN_PROGRESS", "REPORTING" -> MandateSetupStep.EXECUTING
        else -> MandateSetupStep.UNKNOWN
    }

    MandateStatus.CHARGING,
    MandateStatus.CHECKOUT_IN_PROGRESS,
    MandateStatus.REPORTING,
    -> MandateSetupStep.EXECUTING

    MandateStatus.SUCCEEDED -> MandateSetupStep.PROOF_COMPLETE
    MandateStatus.CONSUMED -> if (
        merchantOutcome == com.wishtrace.app.domain.MandateMerchantOutcome.DECLINED
    ) {
        MandateSetupStep.PROOF_DECLINED
    } else {
        MandateSetupStep.PROOF_COMPLETE
    }

    MandateStatus.DECLINED -> if (lastChargeState == null) {
        MandateSetupStep.DECLINED
    } else {
        MandateSetupStep.PROOF_DECLINED
    }
    MandateStatus.CANCELLED -> MandateSetupStep.CANCELLED
    MandateStatus.EXPIRED -> MandateSetupStep.EXPIRED
    MandateStatus.FAILED -> MandateSetupStep.FAILED
    MandateStatus.PAUSED -> MandateSetupStep.READY_TO_ARM
    MandateStatus.UNKNOWN -> MandateSetupStep.UNKNOWN
}

private fun sandboxBilling(email: String) = BillingContact(
    email = email,
    firstName = "Sandbox",
    lastName = "Buyer",
    addressLine1 = "1 Test Checkout Way",
    addressLine2 = null,
    city = "Portland",
    region = "OR",
    postalCode = "97205",
    countryCode = "US",
    phone = null,
)
