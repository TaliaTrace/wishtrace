package com.wishtrace.app.ui.screens.mandate

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
    onArmed: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    MandateSetupScreen(
        state = state,
        onBack = onBack,
        onArm = { viewModel.arm(candidateId) },
        onRefresh = viewModel::refresh,
        onExecuteSandboxProof = { viewModel.executeSandboxProof(verifiedEmail) },
        onArmed = onArmed,
    )
}

@Composable
fun MandateSetupScreen(
    state: MandateSetupUiState,
    onBack: () -> Unit,
    onArm: () -> Unit,
    onRefresh: () -> Unit,
    onExecuteSandboxProof: () -> Unit,
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
                onRefresh = onRefresh,
                onExecuteSandboxProof = onExecuteSandboxProof,
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
                    MandateSetupStep.ACTIVE -> Armed(state, onArmed = onArmed)
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
                        message = "Refresh for the authoritative state.",
                    )
                    MandateSetupStep.UNKNOWN -> TerminalState(
                        title = "Result still unknown",
                        message = "WishTrace will not claim the autopilot is on. Refresh to reconcile.",
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
                text = "Approve once. Never miss a moment again.",
                modifier = Modifier.semantics { heading() },
                color = Ink,
                style = MaterialTheme.typography.headlineLarge,
            )
            Text(
                text = "One passkey arms a delegated budget. When the occasion nears, " +
                    "WishTrace handles the gift within the cap you set.",
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
private fun Armed(state: MandateSetupUiState, onArmed: () -> Unit) {
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
                "Jackbox checkout. Do not close the app.",
            color = InkMuted,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
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
    TerminalState(
        title = if (merchantAttempted) "Sandbox merchant attempt recorded" else "Sandbox charge declined",
        message = if (merchantAttempted) {
            "The real merchant declined the tokenized sandbox card, as expected. No order was created."
        } else {
            "Prava declined the delegated sandbox charge before a merchant order was created."
        },
    )
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
    onRefresh: () -> Unit,
    onExecuteSandboxProof: () -> Unit,
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
                    PrimaryAction(
                        text = "Run sandbox merchant proof",
                        onClick = onExecuteSandboxProof,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !state.busy,
                    )
                    SecondaryAction(
                        text = "Do this later",
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

                MandateSetupStep.EXECUTING -> SecondaryAction(
                    text = "Merchant attempt in progress…",
                    onClick = {},
                    modifier = Modifier.fillMaxWidth(),
                    enabled = false,
                )

                MandateSetupStep.PROOF_COMPLETE,
                MandateSetupStep.PROOF_DECLINED,
                -> PrimaryAction(
                    text = "Done",
                    onClick = onArmed,
                    modifier = Modifier.fillMaxWidth(),
                )

                MandateSetupStep.CANCELLED,
                MandateSetupStep.EXPIRED,
                MandateSetupStep.FAILED,
                -> PrimaryAction(
                    text = "Try again",
                    onClick = onArm,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.busy,
                )

                MandateSetupStep.DECLINED,
                MandateSetupStep.UNKNOWN,
                -> SecondaryAction(
                    text = "Refresh result",
                    onClick = onRefresh,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.busy,
                )

                else -> Unit
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
