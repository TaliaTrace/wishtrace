package com.wishtrace.app.ui.screens.checkout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.booleanResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.wishtrace.app.R
import com.wishtrace.app.domain.VerifiedResult
import com.wishtrace.app.ui.BillingForm
import com.wishtrace.app.ui.CheckoutStep
import com.wishtrace.app.ui.CheckoutUiState
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
fun CheckoutScreen(
    state: CheckoutUiState,
    verifiedEmail: String?,
    onBack: () -> Unit,
    onBillingChange: (BillingForm) -> Unit,
    onUseSandboxBilling: () -> Unit,
    onQuote: () -> Unit,
    onApprove: () -> Unit,
    onRefresh: () -> Unit,
    onMessageChange: (String) -> Unit,
    onSaveMessage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        containerColor = Canvas,
        topBar = {
            ScreenTopBar(
                title = when (state.step) {
                    CheckoutStep.AUTHORIZATION_DECLINED,
                    CheckoutStep.ORDER_SUCCEEDED,
                    -> "Result"

                    else -> "Review"
                },
                onBack = onBack,
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp),
            )
        },
        bottomBar = {
            CheckoutActionBar(
                state = state,
                onQuote = onQuote,
                onApprove = onApprove,
                onRefresh = onRefresh,
                onSaveMessage = onSaveMessage,
            )
        },
    ) { innerPadding ->
        when (state.step) {
            CheckoutStep.IDLE,
            CheckoutStep.LOADING,
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
                state.intent?.let { intent ->
                    ProductSummary(
                        merchant = intent.merchantName,
                        title = intent.title,
                        variant = intent.variantTitle,
                        amount = (intent.approvedTotal ?: intent.itemPrice).formatted(Locale.US),
                    )
                }
                state.error?.let { ErrorBanner(it, Modifier.fillMaxWidth()) }
                when (state.step) {
                    CheckoutStep.REVIEW,
                    CheckoutStep.QUOTING,
                    -> BillingSection(
                        form = state.billing,
                        verifiedEmail = verifiedEmail,
                        enabled = !state.busy,
                        onChange = onBillingChange,
                        onUseSandboxBilling = onUseSandboxBilling,
                    )

                    CheckoutStep.READY_FOR_APPROVAL,
                    CheckoutStep.CREATING_APPROVAL,
                    -> ApprovalReview(state)

                    CheckoutStep.AWAITING_APPROVAL,
                    CheckoutStep.RECONCILING,
                    -> WaitingForApproval(state.step == CheckoutStep.RECONCILING)

                    CheckoutStep.AUTHORIZATION_DECLINED,
                    CheckoutStep.ORDER_SUCCEEDED,
                    -> ResultSection(
                        result = state.result,
                        messageText = state.messageText,
                        messageSaved = state.messageSaved,
                        onMessageChange = onMessageChange,
                    )

                    CheckoutStep.CANCELLED -> RecoveryState(
                        title = "Approval cancelled",
                        message = "No merchant checkout was started.",
                    )

                    CheckoutStep.EXPIRED -> RecoveryState(
                        title = "Approval expired",
                        message = "Refresh the live quote before trying again.",
                    )

                    CheckoutStep.UNKNOWN -> RecoveryState(
                        title = "Result still unknown",
                        message = "WishTrace will not retry the merchant payment. Refresh to reconcile it.",
                    )

                    CheckoutStep.FAILED -> RecoveryState(
                        title = "Checkout did not complete",
                        message = "No successful order is being claimed. Refresh for the authoritative state.",
                    )

                }
            }
        }
    }
}

@Composable
private fun ProductSummary(
    merchant: String,
    title: String,
    variant: String?,
    amount: String,
) {
    Surface(
        color = SurfaceWhite,
        shape = RoundedCornerShape(24.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, OutlineCool),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                color = LavenderSurface,
                contentColor = BrandIndigo,
                shape = RoundedCornerShape(18.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.CreditCard,
                    contentDescription = null,
                    modifier = Modifier.padding(16.dp),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(merchant, color = InkMuted, style = MaterialTheme.typography.labelMedium)
                Text(title, color = Ink, style = MaterialTheme.typography.titleMedium)
                variant?.let {
                    Text(it, color = InkMuted, style = MaterialTheme.typography.bodySmall)
                }
            }
            Text(
                amount,
                color = Ink,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}

@Composable
private fun BillingSection(
    form: BillingForm,
    verifiedEmail: String?,
    enabled: Boolean,
    onChange: (BillingForm) -> Unit,
    onUseSandboxBilling: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Billing details",
            modifier = Modifier.semantics { heading() },
            color = Ink,
            style = MaterialTheme.typography.headlineSmall,
        )
        Text(
            text = "Used once for the live merchant quote. WishTrace does not store this address.",
            color = InkMuted,
            style = MaterialTheme.typography.bodyMedium,
        )
        if (booleanResource(R.bool.wishtrace_sandbox_tools)) {
            SecondaryAction(
                text = "Use sandbox test address",
                onClick = onUseSandboxBilling,
                enabled = enabled,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = "Fills a synthetic test address for the Prava expected-failure proof. " +
                    "Not a real address.",
                color = InkMuted,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Surface(color = BlueSurface, shape = RoundedCornerShape(16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(9.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.Lock, contentDescription = null, tint = BrandBlue)
                Text(
                    text = verifiedEmail ?: "Verified Google email required",
                    color = Ink,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            CheckoutField(
                value = form.firstName,
                label = "First name",
                enabled = enabled,
                onValueChange = { onChange(form.copy(firstName = it)) },
                modifier = Modifier.weight(1f),
            )
            CheckoutField(
                value = form.lastName,
                label = "Last name",
                enabled = enabled,
                onValueChange = { onChange(form.copy(lastName = it)) },
                modifier = Modifier.weight(1f),
            )
        }
        CheckoutField(
            value = form.addressLine1,
            label = "Address",
            enabled = enabled,
            onValueChange = { onChange(form.copy(addressLine1 = it)) },
        )
        CheckoutField(
            value = form.addressLine2,
            label = "Apartment / suite (optional)",
            enabled = enabled,
            onValueChange = { onChange(form.copy(addressLine2 = it)) },
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            CheckoutField(
                value = form.city,
                label = "City",
                enabled = enabled,
                onValueChange = { onChange(form.copy(city = it)) },
                modifier = Modifier.weight(1f),
            )
            CheckoutField(
                value = form.region,
                label = "State / region",
                enabled = enabled,
                onValueChange = { onChange(form.copy(region = it)) },
                modifier = Modifier.weight(1f),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            CheckoutField(
                value = form.postalCode,
                label = "Postal code",
                enabled = enabled,
                onValueChange = { onChange(form.copy(postalCode = it)) },
                modifier = Modifier.weight(1f),
            )
            CheckoutField(
                value = form.countryCode,
                label = "Country code",
                enabled = enabled,
                onValueChange = { onChange(form.copy(countryCode = it.take(2).uppercase())) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun CheckoutField(
    value: String,
    label: String,
    enabled: Boolean,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        singleLine = true,
        label = { Text(label) },
        shape = RoundedCornerShape(16.dp),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
    )
}

@Composable
private fun ApprovalReview(state: CheckoutUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(
            text = "Exact total confirmed",
            modifier = Modifier.semantics { heading() },
            color = Ink,
            style = MaterialTheme.typography.headlineSmall,
        )
        state.intent?.deliverySummary?.let {
            FactRow("Delivery", it)
        }
        FactRow("Approval", "Prava sandbox opens in a secure browser tab")
        FactRow("After approval", "WishTrace attempts this merchant checkout once")
        Surface(color = LavenderSurface, shape = RoundedCornerShape(18.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.Lock, contentDescription = null, tint = BrandIndigo)
                Text(
                    "Card credentials never enter the Android app.",
                    color = Ink,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
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
private fun WaitingForApproval(reconciling: Boolean) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        CircularProgressIndicator(color = BrandIndigo)
        Text(
            if (reconciling) "Verifying the result…" else "Waiting for Prava",
            modifier = Modifier.semantics { heading() },
            color = Ink,
            style = MaterialTheme.typography.headlineMedium,
        )
        Text(
            "Only the backend’s reconciled merchant and Prava state can complete this flow.",
            color = InkMuted,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun ResultSection(
    result: VerifiedResult?,
    messageText: String,
    messageSaved: Boolean,
    onMessageChange: (String) -> Unit,
) {
    if (result == null) {
        WaitingForApproval(reconciling = true)
        return
    }
    val success = result is VerifiedResult.OrderReceipt
    Surface(
        color = if (success) SuccessSurface else BlueSurface,
        shape = RoundedCornerShape(24.dp),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                imageVector = if (success) {
                    Icons.Rounded.CheckCircle
                } else {
                    Icons.AutoMirrored.Rounded.ReceiptLong
                },
                contentDescription = null,
                tint = if (success) Success else BrandBlue,
            )
            Text(
                text = if (success) "Gift secured" else "Sandbox card tested",
                modifier = Modifier.semantics { heading() },
                color = Ink,
                style = MaterialTheme.typography.headlineMedium,
            )
            Text(
                text = when (result) {
                    is VerifiedResult.AuthorizationDeclined -> result.message
                    is VerifiedResult.OrderReceipt -> "Merchant order ${result.merchantOrderId} was verified."
                },
                color = InkMuted,
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "Add your note",
            modifier = Modifier.semantics { heading() },
            color = Ink,
            style = MaterialTheme.typography.titleLarge,
        )
        OutlinedTextField(
            value = messageText,
            onValueChange = onMessageChange,
            modifier = Modifier.fillMaxWidth(),
            minLines = 4,
            maxLines = 7,
            label = { Text("Message") },
            supportingText = {
                Text(if (messageSaved) "Saved to this purchase" else "${messageText.length}/500")
            },
            shape = RoundedCornerShape(18.dp),
        )
    }
}

@Composable
private fun RecoveryState(title: String, message: String) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 40.dp),
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
private fun CheckoutActionBar(
    state: CheckoutUiState,
    onQuote: () -> Unit,
    onApprove: () -> Unit,
    onRefresh: () -> Unit,
    onSaveMessage: () -> Unit,
) {
    Surface(color = SurfaceWhite, shadowElevation = 8.dp) {
        Column(
            modifier = Modifier
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
        ) {
            when (state.step) {
                CheckoutStep.REVIEW,
                CheckoutStep.QUOTING,
                -> PrimaryAction(
                    text = if (state.busy) "Checking live total…" else "Check exact total",
                    onClick = onQuote,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.busy,
                )

                CheckoutStep.READY_FOR_APPROVAL,
                CheckoutStep.CREATING_APPROVAL,
                -> PrimaryAction(
                    text = if (state.busy) {
                        "Opening Prava…"
                    } else {
                        "Approve ${state.intent?.approvedTotal?.formatted(Locale.US).orEmpty()} with Prava"
                    },
                    onClick = onApprove,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.busy,
                )

                CheckoutStep.AWAITING_APPROVAL,
                CheckoutStep.RECONCILING,
                CheckoutStep.CANCELLED,
                CheckoutStep.EXPIRED,
                CheckoutStep.FAILED,
                CheckoutStep.UNKNOWN,
                -> SecondaryAction(
                    text = if (state.busy) "Verifying…" else "Refresh result",
                    onClick = onRefresh,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.busy,
                )

                CheckoutStep.AUTHORIZATION_DECLINED,
                CheckoutStep.ORDER_SUCCEEDED,
                -> PrimaryAction(
                    text = if (state.messageSaved) "Note saved" else "Save note",
                    onClick = onSaveMessage,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = state.messageText.isNotBlank() && !state.messageSaved,
                )

                else -> Unit
            }
        }
    }
}
