package com.wishtrace.app.ui.screens.mandate

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.booleanResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wishtrace.app.R
import com.wishtrace.app.domain.MandateMerchantOutcome
import com.wishtrace.app.ui.MandateSetupStep
import com.wishtrace.app.ui.MandateSetupUiState
import com.wishtrace.app.ui.MandateSetupViewModel
import com.wishtrace.app.ui.WishTraceTestTags
import com.wishtrace.app.ui.components.ErrorBanner
import com.wishtrace.app.ui.components.PrimaryAction
import com.wishtrace.app.ui.components.ScreenTopBar
import com.wishtrace.app.ui.components.SecondaryAction
import com.wishtrace.app.ui.theme.BlueSurface
import com.wishtrace.app.ui.theme.BrandBlue
import com.wishtrace.app.ui.theme.BrandIndigo
import com.wishtrace.app.ui.theme.Canvas
import com.wishtrace.app.ui.theme.Ink
import com.wishtrace.app.ui.theme.InkMuted
import com.wishtrace.app.ui.theme.LavenderSurface
import com.wishtrace.app.ui.theme.OutlineCool
import com.wishtrace.app.ui.theme.Success
import com.wishtrace.app.ui.theme.SuccessSurface
import com.wishtrace.app.ui.theme.SurfaceWhite
import java.util.Locale

@Composable
fun MandateSetupRoute(
    viewModel: MandateSetupViewModel,
    occasionId: String,
    candidateId: String,
    verifiedEmail: String?,
    onBack: () -> Unit,
    onChooseAnotherGift: () -> Unit,
    onArmed: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val sandboxTools = booleanResource(R.bool.wishtrace_sandbox_tools)

    LaunchedEffect(
        sandboxTools,
        state.step,
        state.mandate?.id,
        verifiedEmail,
    ) {
        if (
            sandboxTools &&
            state.step == MandateSetupStep.ACTIVE &&
            state.mandate?.lastChargeState == null
        ) {
            viewModel.executeSandboxProof(verifiedEmail)
        }
    }

    LaunchedEffect(state.freshSelectionReady) {
        if (state.freshSelectionReady) {
            viewModel.consumeFreshSelectionReady()
            onChooseAnotherGift()
        }
    }

    MandateSetupScreen(
        state = state,
        onBack = onBack,
        onArm = { viewModel.arm(candidateId) },
        onRetryApproval = { viewModel.retryApproval(candidateId) },
        onRetryCardIssue = { viewModel.retryCardIssue(verifiedEmail) },
        onRefresh = viewModel::refresh,
        onChooseAnotherGift = viewModel::chooseAnotherGift,
        onArmed = onArmed,
    )
}

@Composable
fun MandateSetupScreen(
    state: MandateSetupUiState,
    onBack: () -> Unit,
    onArm: () -> Unit,
    onRetryApproval: () -> Unit,
    onRetryCardIssue: () -> Unit,
    onRefresh: () -> Unit,
    onChooseAnotherGift: () -> Unit,
    onArmed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sandboxTools = booleanResource(R.bool.wishtrace_sandbox_tools)
    Scaffold(
        modifier = modifier.testTag(WishTraceTestTags.MandateSetupScreen),
        containerColor = Canvas,
        topBar = {
            ScreenTopBar(
                title = "Autopilot",
                onBack = onBack,
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp),
            )
        },
        bottomBar = {
            MandateActionBar(
                state = state,
                onArm = onArm,
                onRetryApproval = onRetryApproval,
                onRetryCardIssue = onRetryCardIssue,
                onRefresh = onRefresh,
                onChooseAnotherGift = onChooseAnotherGift,
                onArmed = onArmed,
                sandboxTools = sandboxTools,
            )
        },
    ) { innerPadding ->
        when (state.step) {
            MandateSetupStep.IDLE,
            MandateSetupStep.LOADING,
            -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = BrandIndigo)
            }

            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                state.error?.let { ErrorBanner(it, Modifier.fillMaxWidth()) }
                when (state.step) {
                    MandateSetupStep.READY_TO_ARM -> ReadyToArm(state)
                    MandateSetupStep.SETTING_UP,
                    MandateSetupStep.AWAITING_APPROVAL,
                    -> AwaitingApproval(state)
                    MandateSetupStep.REFRESHING -> Refreshing(state)
                    MandateSetupStep.ACTIVE -> Armed(state)
                    MandateSetupStep.PROOF_BLOCKED -> ProofBlocked(state)
                    MandateSetupStep.EXECUTING -> ExecutingProof()
                    MandateSetupStep.PROOF_COMPLETE -> ProofComplete(state)
                    MandateSetupStep.PROOF_DECLINED -> ProofDeclined(state)
                    MandateSetupStep.DECLINED -> TerminalState(
                        title = "Autopilot declined",
                        message = "Prava did not approve this mandate. Nothing was charged.",
                    )
                    MandateSetupStep.CANCELLED -> TerminalState(
                        title = "Autopilot cancelled",
                        message = "No mandate was created. You can start again.",
                    )
                    MandateSetupStep.EXPIRED -> TerminalState(
                        title = "Approval expired",
                        message = "Refresh to re-arm the autopilot before the occasion passes.",
                    )
                    MandateSetupStep.FAILED -> TerminalState(
                        title = "Autopilot did not arm",
                        message = when (state.mandate?.setupFailureCode) {
                            "PROVISION_ERROR" ->
                                "Prava could not provision this sandbox card. No mandate or " +
                                    "merchant charge was created."
                            "DEVICE_BINDING_FAILED" ->
                                "Prava could not bind this device or passkey. No mandate or " +
                                    "merchant charge was created."
                            "FIDO_START_FAILED" ->
                                "Prava could not start the passkey check. No mandate, one-time " +
                                    "card or merchant attempt was created. Check this phone's " +
                                    "screen lock and Chrome, then retry approval."
                            "AUTH_FAILED" ->
                                "Visa and Prava could not finish the passkey check. No mandate, " +
                                    "one-time card or merchant attempt was created."
                            "AUTH_CANCELLED" ->
                                "The passkey window closed before approval. No mandate, one-time " +
                                    "card or merchant attempt was created."
                            else -> if (state.mandate?.lastProviderStatus == "failed") {
                                "Prava returned a failed sandbox setup. No mandate or merchant " +
                                    "charge was created."
                            } else {
                                "Prava could not complete setup. Nothing was charged."
                            }
                        },
                    )
                    MandateSetupStep.UNKNOWN -> TerminalState(
                        title = "Result still unknown",
                        message = "The merchant result could not be confirmed, so this one-time " +
                            "card is locked and will not be reused. You can choose a different " +
                            "gift in a separate approval.",
                    )
                }
            }
        }
    }
}

@Composable
private fun ReadyToArm(state: MandateSetupUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Text(
                text = "Approve this gift once.",
                modifier = Modifier.semantics { heading() },
                color = Ink,
                style = MaterialTheme.typography.headlineLarge,
            )
            Text(
                text = "One passkey authorizes this exact gift within your cap. WishTrace " +
                    "can then run checkout automatically without asking again for this purchase.",
                color = InkMuted,
                style = MaterialTheme.typography.bodyLarge,
            )
        }
        Surface(
            color = BlueSurface,
            shape = RoundedCornerShape(24.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(13.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    modifier = Modifier.size(48.dp),
                    color = SurfaceWhite,
                    contentColor = BrandBlue,
                    shape = CircleShape,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.Shield,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = state.mandate?.let { mandateSummary(it) }
                            ?: "Delegated spend authority",
                        color = Ink,
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                        text = "Stays within the cap. The card never leaves Prava.",
                        color = InkMuted,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
        Surface(
            color = LavenderSurface,
            shape = RoundedCornerShape(18.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.Lock, contentDescription = null, tint = BrandIndigo)
                Text(
                    text = "Card credentials never enter the Android app.",
                    color = Ink,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
private fun AwaitingApproval(state: MandateSetupUiState) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        CircularProgressIndicator(color = BrandIndigo)
        Text(
            text = if (state.step == MandateSetupStep.SETTING_UP) {
                "Opening Prava…"
            } else {
                "Waiting for your approval"
            },
            modifier = Modifier.semantics { heading() },
            color = Ink,
            style = MaterialTheme.typography.headlineMedium,
        )
        Text(
            text = "Choose or enter the card in Prava, then approve the mandate with a passkey. " +
                "WishTrace can act later only inside this exact cap.",
            color = InkMuted,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun Refreshing(state: MandateSetupUiState) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        CircularProgressIndicator(color = BrandIndigo)
        Text(
            text = "Verifying the autopilot…",
            modifier = Modifier.semantics { heading() },
            color = Ink,
            style = MaterialTheme.typography.headlineMedium,
        )
        Text(
            text = "Only the backend’s reconciled Prava state can confirm this.",
            color = InkMuted,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun Armed(state: MandateSetupUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Surface(
            color = SuccessSurface,
            shape = RoundedCornerShape(24.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = null,
                    tint = Success,
                    modifier = Modifier.size(32.dp),
                )
                Text(
                    text = "Autopilot is on",
                    modifier = Modifier
                        .semantics { heading() }
                        .testTag(WishTraceTestTags.MandateArmedConfirmation),
                    color = Ink,
                    style = MaterialTheme.typography.headlineMedium,
                )
                state.mandate?.let {
                    Text(
                        text = mandateSummary(it),
                        color = InkMuted,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
        }
        Surface(
            color = SurfaceWhite,
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, OutlineCool),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FactRow("Cap", state.mandate?.let { capLabel(it) } ?: "Your set budget")
                FactRow(
                    "Frequency",
                    state.mandate?.recurringFrequency?.replace('_', ' ')?.replaceFirstChar(Char::uppercase)
                        ?: "As set",
                )
                state.mandate?.lastProviderStatus?.let {
                    FactRow("Prava status", it)
                }
            }
        }
        Text(
            text = "Armed means permission is active — not that a gift has been purchased.",
            color = InkMuted,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun ExecutingProof() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        CircularProgressIndicator(color = BrandIndigo)
        Text(
            text = "Running the real merchant proof…",
            modifier = Modifier.semantics { heading() },
            color = Ink,
            style = MaterialTheme.typography.headlineMedium,
        )
        Text(
            text = "WishTrace is requesting one bounded Prava token and attempting the exact " +
                "Jackbox checkout automatically. Do not close the app.",
            color = InkMuted,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun ProofBlocked(state: MandateSetupUiState) {
    val mandate = state.mandate
    val total = mandate?.lastChargeAmountMinor?.asUsd()
    val cap = mandate?.approvedAmountMinor?.asUsd()
    val overCap = mandate?.lastChargeFailureCode == "MERCHANT_TOTAL_EXCEEDS_MANDATE"
    TerminalState(
        title = if (overCap) "Live total crossed your cap" else "Checkout stopped safely",
        message = if (overCap && total != null && cap != null) {
            "$total is above the $cap limit you approved. No one-time card was requested. " +
                "Choose another gift or raise the budget before approving a new limit."
        } else {
            "The approved mandate is still safe, but WishTrace could not start this merchant " +
                "attempt. No order was created."
        },
    )
}

@Composable
private fun ProofComplete(state: MandateSetupUiState) {
    val orderId = state.mandate?.merchantOrderId
    TerminalState(
        title = "Sandbox order verified",
        message = if (orderId == null) {
            "Prava and the merchant reported completion, but no order identifier was returned."
        } else {
            "The merchant returned verified order $orderId. Sandbox funds only; no real card was charged."
        },
    )
}

@Composable
private fun ProofDeclined(state: MandateSetupUiState) {
    val merchantAttempted = state.mandate?.merchantOutcome == MandateMerchantOutcome.DECLINED
    val canRetryCard = state.mandate?.mintRetryAvailable == true
    if (!merchantAttempted) {
        TerminalState(
            title = "Prava couldn't make the card",
            message = if (canRetryCard) {
            "Your approval is still active. No payment was submitted to Jackbox. You can try " +
                "issuing the sandbox card once more without another passkey."
            } else if (state.canChooseAnotherSandboxCard) {
            "Prava couldn't create the one-time card after two tries. Choose another gift, then " +
                "select a different approved test card in Prava."
            } else {
            "No card was issued, no payment was submitted to Jackbox, and nothing was purchased."
            },
        )
        return
    }

    val mandate = state.mandate
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = BrandIndigo,
            contentColor = SurfaceWhite,
            shape = RoundedCornerShape(28.dp),
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                Surface(
                    modifier = Modifier.size(48.dp),
                    color = SurfaceWhite.copy(alpha = 0.16f),
                    contentColor = SurfaceWhite,
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(27.dp),
                        )
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Sandbox proof captured",
                        modifier = Modifier.semantics { heading() },
                        style = MaterialTheme.typography.headlineLarge,
                    )
                    Text(
                        text = "Expected processor decline",
                        color = SurfaceWhite.copy(alpha = 0.78f),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(132.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Surface(
                modifier = Modifier
                    .weight(1.35f)
                    .fillMaxSize(),
                color = BlueSurface,
                contentColor = Ink,
                shape = RoundedCornerShape(24.dp),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Storefront,
                        contentDescription = null,
                        tint = BrandBlue,
                    )
                    Column {
                        Text(
                            text = mandate.merchantName,
                            color = InkMuted,
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                        )
                        Text(
                            text = mandate.lastChargeAmountMinor?.asUsd()
                                ?: mandate.itemPriceMinor.asUsd(),
                            style = MaterialTheme.typography.headlineMedium,
                            maxLines = 1,
                        )
                    }
                }
            }
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize(),
                color = LavenderSurface,
                contentColor = Ink,
                shape = RoundedCornerShape(24.dp),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = null,
                        tint = BrandIndigo,
                    )
                    Column {
                        Text("ORDER", color = InkMuted, style = MaterialTheme.typography.labelSmall)
                        Text("Not created", style = MaterialTheme.typography.titleSmall)
                    }
                }
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = SuccessSurface,
            contentColor = Success,
            shape = RoundedCornerShape(22.dp),
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.AutoMirrored.Rounded.ReceiptLong, contentDescription = null)
                Column(modifier = Modifier.weight(1f)) {
                    Text("PRAVA RECORDED", style = MaterialTheme.typography.labelSmall)
                    Text(
                        "No real money moved",
                        color = Ink,
                        style = MaterialTheme.typography.titleSmall,
                    )
                }
            }
        }
    }
}

@Composable
private fun TerminalState(title: String, message: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            title,
            modifier = Modifier.semantics { heading() },
            color = Ink,
            style = MaterialTheme.typography.headlineMedium,
        )
        Text(message, color = InkMuted, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun FactRow(label: String, value: String) {
    Column {
        Text(label, color = InkMuted, style = MaterialTheme.typography.labelMedium)
        Text(value, color = Ink, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun MandateActionBar(
    state: MandateSetupUiState,
    onArm: () -> Unit,
    onRetryApproval: () -> Unit,
    onRetryCardIssue: () -> Unit,
    onRefresh: () -> Unit,
    onChooseAnotherGift: () -> Unit,
    onArmed: () -> Unit,
    sandboxTools: Boolean,
) {
    Surface(color = SurfaceWhite, shadowElevation = 8.dp) {
        Column(
            modifier = Modifier
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            when (state.step) {
                MandateSetupStep.READY_TO_ARM -> PrimaryAction(
                    text = "Approve with one passkey",
                    onClick = onArm,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(WishTraceTestTags.MandateSetupCta),
                    enabled = !state.busy,
                )

                MandateSetupStep.SETTING_UP,
                MandateSetupStep.AWAITING_APPROVAL,
                MandateSetupStep.REFRESHING,
                -> SecondaryAction(
                    text = if (state.busy) "Verifying…" else "Refresh result",
                    onClick = onRefresh,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.busy,
                )

                MandateSetupStep.ACTIVE -> if (sandboxTools) {
                    SecondaryAction(
                        text = "Starting merchant proof…",
                        onClick = {},
                        modifier = Modifier.fillMaxWidth(),
                        enabled = false,
                    )
                } else {
                    PrimaryAction(
                        text = "Done",
                        onClick = onArmed,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                MandateSetupStep.EXECUTING -> SecondaryAction(
                    text = "Merchant attempt in progress…",
                    onClick = {},
                    modifier = Modifier.fillMaxWidth(),
                    enabled = false,
                )

                MandateSetupStep.PROOF_BLOCKED -> PrimaryAction(
                    text = "Choose another gift",
                    onClick = onChooseAnotherGift,
                    modifier = Modifier.fillMaxWidth(),
                )

                MandateSetupStep.PROOF_COMPLETE -> PrimaryAction(
                    text = "Done",
                    onClick = onArmed,
                    modifier = Modifier.fillMaxWidth(),
                )

                MandateSetupStep.PROOF_DECLINED -> if (
                    state.mandate?.mintRetryAvailable == true
                ) {
                    PrimaryAction(
                        text = "Try card again",
                        onClick = onRetryCardIssue,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    SecondaryAction(
                        text = "Not now",
                        onClick = onArmed,
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else if (sandboxTools && state.canChooseAnotherSandboxCard) {
                    PrimaryAction(
                        text = "Use a new sandbox card",
                        onClick = onChooseAnotherGift,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    SecondaryAction(
                        text = "Done",
                        onClick = onArmed,
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    PrimaryAction(
                        text = "Done",
                        onClick = onArmed,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                MandateSetupStep.FAILED -> if (state.canRetryApproval) {
                    PrimaryAction(
                        text = "Retry Prava approval",
                        onClick = onRetryApproval,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !state.busy,
                    )
                    SecondaryAction(
                        text = "Choose another gift",
                        onClick = onArmed,
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    PrimaryAction(
                        text = "Done",
                        onClick = onArmed,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                MandateSetupStep.CANCELLED,
                MandateSetupStep.EXPIRED,
                MandateSetupStep.DECLINED,
                -> PrimaryAction(
                    text = "Done",
                    onClick = onArmed,
                    modifier = Modifier.fillMaxWidth(),
                )

                MandateSetupStep.UNKNOWN -> {
                    PrimaryAction(
                        text = "Choose another gift",
                        onClick = onChooseAnotherGift,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    SecondaryAction(
                        text = "Refresh result",
                        onClick = onRefresh,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !state.busy,
                    )
                }

                MandateSetupStep.IDLE,
                MandateSetupStep.LOADING,
                -> SecondaryAction(
                    text = "Loading…",
                    onClick = {},
                    modifier = Modifier.fillMaxWidth(),
                    enabled = false,
                )
            }
        }
    }
}

private fun mandateSummary(mandate: com.wishtrace.app.domain.MandateDetails): String =
    "${mandate.merchantName} · ${mandate.productTitle} · ${capLabel(mandate)}"

private fun capLabel(mandate: com.wishtrace.app.domain.MandateDetails): String =
    mandate.approvedAmountMinor.asUsd()

private fun Int.asUsd(): String {
    val dollars = this / 100
    val cents = (this % 100).toString().padStart(2, '0')
    return "$$dollars.$cents"
}
