package com.wishtrace.app.ui.screens.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wishtrace.app.data.PreviewFixtures
import com.wishtrace.app.domain.HomeSnapshot
import com.wishtrace.app.domain.MandateDetails
import com.wishtrace.app.domain.MandateMerchantOutcome
import com.wishtrace.app.domain.MandateStatus
import com.wishtrace.app.ui.HomeUiState
import com.wishtrace.app.ui.WishTraceTestTags
import com.wishtrace.app.ui.components.PrimaryAction
import com.wishtrace.app.ui.components.RecipientAvatar
import com.wishtrace.app.ui.components.StaggeredEntrance
import com.wishtrace.app.ui.components.WishTraceWordmark
import com.wishtrace.app.ui.theme.BlueSurface
import com.wishtrace.app.ui.theme.BrandBlue
import com.wishtrace.app.ui.theme.BrandIndigo
import com.wishtrace.app.ui.theme.Ink
import com.wishtrace.app.ui.theme.InkMuted
import com.wishtrace.app.ui.theme.LavenderSurface
import com.wishtrace.app.ui.theme.OutlineCool
import com.wishtrace.app.ui.theme.SurfaceWhite
import com.wishtrace.app.ui.theme.Success
import com.wishtrace.app.ui.theme.SuccessSurface
import com.wishtrace.app.ui.theme.WishTraceTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun HomeScreen(
    state: HomeUiState,
    giverDisplayName: String? = null,
    onRetry: () -> Unit,
    onFindGift: () -> Unit,
    onFindAnotherGift: () -> Unit = onFindGift,
    onReviewRecipient: () -> Unit,
    onAddPerson: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag(WishTraceTestTags.HomeScreen),
    ) {
        when (state) {
            HomeUiState.Loading -> HomeLoading()
            HomeUiState.Empty -> HomeEmpty(onAddPerson = onAddPerson)
            is HomeUiState.Error -> HomeError(
                message = state.message,
                onRetry = onRetry,
            )

            is HomeUiState.Content -> HomeContent(
                snapshot = state.snapshot,
                mandate = state.mandate,
                giverDisplayName = giverDisplayName,
                onFindGift = onFindGift,
                onFindAnotherGift = onFindAnotherGift,
                onReviewRecipient = onReviewRecipient,
            )
        }
    }
}

@Composable
private fun HomeContent(
    snapshot: HomeSnapshot,
    mandate: MandateDetails?,
    giverDisplayName: String?,
    onFindGift: () -> Unit,
    onFindAnotherGift: () -> Unit,
    onReviewRecipient: () -> Unit,
) {
    val recipient = snapshot.recipient
    val occasion = snapshot.occasion
    val dateFormatter = DateTimeFormatter.ofPattern("MMM d", Locale.US)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = PaddingValues(
            start = 20.dp,
            end = 20.dp,
            top = 16.dp,
            bottom = 32.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            StaggeredEntrance(index = 0) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        WishTraceWordmark(
                            markSize = 34.dp,
                            textStyle = MaterialTheme.typography.headlineMedium,
                        )
                        Text(
                            text = greetingFor(giverDisplayName),
                            modifier = Modifier.semantics { heading() },
                            color = Ink,
                            style = MaterialTheme.typography.headlineMedium,
                        )
                    }
                    Surface(
                        modifier = Modifier.size(52.dp),
                        color = BlueSurface,
                        contentColor = BrandBlue,
                        shape = RoundedCornerShape(18.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.Favorite,
                                contentDescription = "Thoughtful gifting",
                                modifier = Modifier.size(24.dp),
                            )
                        }
                    }
                }
            }
        }
        item {
            StaggeredEntrance(index = 1) {
                OccasionBentoGrid(
                    snapshot = snapshot,
                    mandate = mandate,
                    dateLabel = occasion.localDate.format(dateFormatter),
                    onFindGift = onFindGift,
                    onFindAnotherGift = onFindAnotherGift,
                    onReviewRecipient = onReviewRecipient,
                )
            }
        }
        if (recipient.hints.isNotEmpty()) {
            item {
                StaggeredEntrance(index = 2) {
                    Surface(
                    onClick = onReviewRecipient,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(WishTraceTestTags.ReviewRecipient),
                        color = SurfaceWhite,
                        shape = RoundedCornerShape(22.dp),
                    border = BorderStroke(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline,
                    ),
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Surface(
                            modifier = Modifier.size(42.dp),
                            color = MaterialTheme.colorScheme.surface,
                            contentColor = BrandBlue,
                            shape = CircleShape,
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.Lightbulb,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }
                            Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                                Text(
                                    text = "CLUE",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = recipient.hints.first().text,
                                    maxLines = 1,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                            contentDescription = "Review ${recipient.displayName}'s profile",
                            modifier = Modifier.size(20.dp),
                            tint = BrandIndigo,
                        )
                    }
                    }
                }
            }
        }
    }
}

@Composable
private fun OccasionBentoGrid(
    snapshot: HomeSnapshot,
    mandate: MandateDetails?,
    dateLabel: String,
    onFindGift: () -> Unit,
    onFindAnotherGift: () -> Unit,
    onReviewRecipient: () -> Unit,
) {
    val recipient = snapshot.recipient
    val occasion = snapshot.occasion
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(228.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Surface(
                onClick = onReviewRecipient,
                modifier = Modifier
                    .weight(1.45f)
                    .fillMaxHeight()
                    .testTag(WishTraceTestTags.ReviewRecipient),
                color = LavenderSurface,
                contentColor = Ink,
                shape = RoundedCornerShape(28.dp),
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "NEXT MOMENT",
                        color = BrandIndigo,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Surface(
                        modifier = Modifier.size(78.dp),
                        color = SurfaceWhite,
                        shape = CircleShape,
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            RecipientAvatar(
                                initials = recipient.initials,
                                photoUri = recipient.photoUri,
                                size = 66.dp,
                            )
                        }
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(
                            text = recipient.displayName,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                        )
                        Text(
                            text = occasion.kind.displayName,
                            color = InkMuted,
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 1,
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CalendarMonth,
                                contentDescription = null,
                                tint = BrandBlue,
                                modifier = Modifier.size(16.dp),
                            )
                            Text(
                                text = dateLabel,
                                color = InkMuted,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
            }
            Column(
                modifier = Modifier
                    .weight(0.9f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    color = BrandIndigo,
                    contentColor = SurfaceWhite,
                    shape = RoundedCornerShape(24.dp),
                ) {
                    Column(
                        modifier = Modifier.padding(15.dp),
                        verticalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("COUNTDOWN", style = MaterialTheme.typography.labelSmall)
                        Text(
                            text = snapshot.daysUntil.toString(),
                            style = MaterialTheme.typography.displayLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        Text("days", style = MaterialTheme.typography.labelLarge)
                    }
                }
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    color = BlueSurface,
                    contentColor = Ink,
                    shape = RoundedCornerShape(24.dp),
                ) {
                    Column(
                        modifier = Modifier.padding(15.dp),
                        verticalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = "GIFT CAP",
                            color = BrandBlue,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = occasion.budget.formatted(Locale.US),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                        )
                        Text("USD", color = InkMuted, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }

        val interests = recipient.interests.take(2)
        if (interests.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 100.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                interests.forEachIndexed { index, interest ->
                    InterestBentoTile(
                        interest = interest,
                        modifier = Modifier.weight(1f),
                        container = if (index == 0) BlueSurface else LavenderSurface,
                    )
                }
                if (interests.size == 1) {
                    InterestBentoTile(
                        interest = occasion.kind.displayName,
                        modifier = Modifier.weight(1f),
                        container = LavenderSurface,
                    )
                }
            }
        }

        mandate?.autopilotStatus()?.let { status ->
            MandateStatusTile(
                status = status,
                onClick = onFindGift,
            )
        }

        PrimaryAction(
            text = when {
                mandate == null -> "Find ${recipient.displayName}'s gift"
                mandate.status.requiresApproval -> "Finish approval"
                mandate.lastChargeState != null -> "Find another gift"
                mandate.isArmed -> "Open autopilot"
                else -> "Review autopilot"
            },
            onClick = if (mandate?.lastChargeState != null) onFindAnotherGift else onFindGift,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(WishTraceTestTags.FindGift),
            trailingContent = {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(19.dp),
                )
            },
        )
    }
}

@Composable
private fun InterestBentoTile(
    interest: String,
    container: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.heightIn(min = 100.dp),
        color = container,
        contentColor = Ink,
        shape = RoundedCornerShape(22.dp),
    ) {
        Column(
            modifier = Modifier.padding(15.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Icon(
                imageVector = interestIcon(interest),
                contentDescription = null,
                tint = BrandIndigo,
                modifier = Modifier.size(24.dp),
            )
            Text(
                text = interest,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun MandateStatusTile(
    status: AutopilotStatusPill,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        color = status.container,
        contentColor = status.content,
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, OutlineCool),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(11.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(42.dp),
                color = SurfaceWhite,
                contentColor = status.content,
                shape = RoundedCornerShape(14.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(status.icon, contentDescription = null, modifier = Modifier.size(21.dp))
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("PRAVA", style = MaterialTheme.typography.labelSmall)
                Text(
                    status.label,
                    color = Ink,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                contentDescription = "Open Prava status",
                modifier = Modifier.size(19.dp),
            )
        }
    }
}

private fun interestIcon(interest: String): ImageVector = when {
    interest.contains("game", ignoreCase = true) -> Icons.Rounded.SportsEsports
    interest.contains("gym", ignoreCase = true) ||
        interest.contains("fitness", ignoreCase = true) -> Icons.Rounded.FitnessCenter
    interest.contains("music", ignoreCase = true) -> Icons.Rounded.Headphones
    interest.contains("book", ignoreCase = true) -> Icons.AutoMirrored.Rounded.MenuBook
    else -> Icons.Rounded.Favorite
}

@Composable
private fun HomeLoading() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator()
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "Preparing your day", style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun HomeEmpty(onAddPerson: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Start with someone you care about.",
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.headlineMedium,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Add one person and their next occasion.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyLarge,
        )
        Spacer(modifier = Modifier.height(20.dp))
        PrimaryAction(
            text = "Add a person",
            onClick = onAddPerson,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun HomeError(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "We lost the thread.",
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.headlineMedium,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyLarge,
        )
        Spacer(modifier = Modifier.height(20.dp))
        PrimaryAction(
            text = "Try again",
            onClick = onRetry,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun HomeContentPreview() {
    WishTraceTheme {
        HomeScreen(
            state = HomeUiState.Content(PreviewFixtures.homeSnapshot()),
            giverDisplayName = "Talia",
            onRetry = {},
            onFindGift = {},
            onReviewRecipient = {},
        )
    }
}

/**
 * Calm autopilot status. Honest about the reconciled backend state: only ACTIVE (or a
 * terminal SUCCEEDED/CONSUMED) reads as "on". Everything else stays pending or muted —
 * no "gift secured" is ever claimed here.
 */
private data class AutopilotStatusPill(
    val label: String,
    val icon: ImageVector,
    val container: Color,
    val content: Color,
)

private fun MandateDetails.autopilotStatus(): AutopilotStatusPill? = when (status) {
    MandateStatus.ACTIVE -> if (lastChargeState != null) {
        AutopilotStatusPill(
            label = if (lastChargeState == "UNKNOWN") "Result unknown" else "Needs attention",
            icon = Icons.Rounded.Schedule,
            container = BlueSurface,
            content = BrandBlue,
        )
    } else {
        AutopilotStatusPill(
            label = "Autopilot on",
            icon = Icons.Rounded.CheckCircle,
            container = SuccessSurface,
            content = Success,
        )
    }

    MandateStatus.SUCCEEDED -> AutopilotStatusPill(
        label = "Handled",
        icon = Icons.Rounded.CheckCircle,
        container = SuccessSurface,
        content = Success,
    )

    MandateStatus.CONSUMED -> if (merchantOutcome == MandateMerchantOutcome.ORDER_VERIFIED) {
        AutopilotStatusPill(
            label = "Handled",
            icon = Icons.Rounded.CheckCircle,
            container = SuccessSurface,
            content = Success,
        )
    } else {
        AutopilotStatusPill(
            label = "Attempt recorded",
            icon = Icons.Rounded.Schedule,
            container = BlueSurface,
            content = BrandBlue,
        )
    }

    MandateStatus.SETUP_CREATING,
    MandateStatus.AWAITING_APPROVAL,
    MandateStatus.CHARGING,
    MandateStatus.CHECKOUT_IN_PROGRESS,
    MandateStatus.REPORTING,
    -> AutopilotStatusPill(
        label = "Awaiting approval",
        icon = Icons.Rounded.Schedule,
        container = BlueSurface,
        content = BrandBlue,
    )

    MandateStatus.DECLINED -> if (lastChargeState != null) {
        AutopilotStatusPill(
            label = "Last attempt failed",
            icon = Icons.Rounded.Schedule,
            container = BlueSurface,
            content = BrandBlue,
        )
    } else {
        null
    }

    // CANCELLED, EXPIRED, FAILED, PAUSED, UNKNOWN — muted; no pill.
    else -> null
}

private fun greetingFor(displayName: String?): String {
    val firstName = displayName
        ?.trim()
        ?.substringBefore(' ')
        ?.takeIf { it.isNotEmpty() && '@' !in it }
    return firstName?.let { "Hi, $it" } ?: "Welcome back"
}
