package com.wishtrace.app.ui.screens.discovery

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.RepeatMode
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.LocalShipping
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.ShoppingBag
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wishtrace.app.R
import com.wishtrace.app.data.PreviewFixtures
import com.wishtrace.app.domain.DiscoveryStage
import com.wishtrace.app.domain.GiftDiscoveryRequest
import com.wishtrace.app.domain.HomeSnapshot
import com.wishtrace.app.ui.DiscoveryUiState
import com.wishtrace.app.ui.HomeUiState
import com.wishtrace.app.ui.WishTraceTestTags
import com.wishtrace.app.ui.components.DimensionalAsset
import com.wishtrace.app.ui.components.PrimaryAction
import com.wishtrace.app.ui.components.RecipientAvatar
import com.wishtrace.app.ui.components.ScreenTopBar
import com.wishtrace.app.ui.components.SecondaryAction
import com.wishtrace.app.ui.components.rememberWishTraceMotionEnabled
import com.wishtrace.app.ui.theme.BlueSurface
import com.wishtrace.app.ui.theme.BrandBlue
import com.wishtrace.app.ui.theme.BrandIndigo
import com.wishtrace.app.ui.theme.BrandIndigoPressed
import com.wishtrace.app.ui.theme.Canvas
import com.wishtrace.app.ui.theme.ErrorRed
import com.wishtrace.app.ui.theme.ErrorSoft
import com.wishtrace.app.ui.theme.Ink
import com.wishtrace.app.ui.theme.InkMuted
import com.wishtrace.app.ui.theme.LavenderSurface
import com.wishtrace.app.ui.theme.OutlineCool
import com.wishtrace.app.ui.theme.Success
import com.wishtrace.app.ui.theme.SuccessSurface
import com.wishtrace.app.ui.theme.SurfaceWhite
import com.wishtrace.app.ui.theme.WishTraceTheme

@Composable
fun GiftDiscoveryScreen(
    homeState: HomeUiState,
    state: DiscoveryUiState,
    onBack: () -> Unit,
    onStart: (GiftDiscoveryRequest) -> Unit,
    onCancel: () -> Unit,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snapshot = (homeState as? HomeUiState.Content)?.snapshot

    BackHandler(enabled = state is DiscoveryUiState.Running) {
        onCancel()
        onBack()
    }

    LaunchedEffect(snapshot?.occasion?.id, state) {
        if (snapshot != null && state == DiscoveryUiState.Idle) {
            onStart(snapshot.toDiscoveryRequest())
        }
    }

    Scaffold(
        modifier = modifier.testTag(WishTraceTestTags.DiscoveryScreen),
        containerColor = Canvas,
        topBar = {
            ScreenTopBar(
                title = snapshot?.recipient?.displayName?.let { "For $it" } ?: "Gift discovery",
                onBack = onBack,
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp),
            )
        },
        bottomBar = {
            if (snapshot != null) {
                DiscoveryActionBar(
                    state = state,
                    recipientName = snapshot.recipient.displayName,
                    onCancel = onCancel,
                    onRetry = { onStart(snapshot.toDiscoveryRequest()) },
                    onContinue = onContinue,
                    onBack = onBack,
                )
            }
        },
    ) { innerPadding ->
        when (homeState) {
            HomeUiState.Loading -> DiscoveryUnavailable(
                title = "Gathering the details",
                message = "Your recipient context is loading.",
                showProgress = true,
                modifier = Modifier.padding(innerPadding),
            )

            HomeUiState.Empty -> DiscoveryUnavailable(
                title = "Add someone first",
                message = "A recipient and occasion are needed to find a fitting gift.",
                showProgress = false,
                modifier = Modifier.padding(innerPadding),
            )

            is HomeUiState.Error -> DiscoveryUnavailable(
                title = "Context unavailable",
                message = homeState.message,
                showProgress = false,
                modifier = Modifier.padding(innerPadding),
            )

            is HomeUiState.Content -> DiscoveryContent(
                snapshot = homeState.snapshot,
                state = state,
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
}

private fun HomeSnapshot.toDiscoveryRequest() = GiftDiscoveryRequest(
    recipientId = recipient.id,
    occasionId = occasion.id,
    budget = occasion.budget,
)

@Composable
private fun DiscoveryContent(
    snapshot: HomeSnapshot,
    state: DiscoveryUiState,
    modifier: Modifier = Modifier,
) {
    val stageIndex = when (state) {
        is DiscoveryUiState.Running ->
            DiscoveryStage.entries.indexOf(state.activeStage).coerceAtLeast(0)

        is DiscoveryUiState.ReadyForRanking -> DiscoveryStage.entries.size
        else -> 0
    }
    val headline = when (state) {
        is DiscoveryUiState.ReadyForRanking -> "Ready for a grounded match."
        DiscoveryUiState.Cancelled -> "Paused right where you left it."
        is DiscoveryUiState.Error -> "That search did not finish."
        else -> "Finding a gift that fits."
    }
    val supporting = when (state) {
        DiscoveryUiState.Idle ->
            "Bringing ${snapshot.recipient.displayName}’s clues into focus."
        is DiscoveryUiState.Running -> state.activeStage.detail(snapshot)
        is DiscoveryUiState.ReadyForRanking ->
            "Product facts stay hidden until their source can be verified."

        DiscoveryUiState.Cancelled -> "The date, budget and clues are still here."
        is DiscoveryUiState.Error -> "Nothing was selected or purchased. Try again safely."
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        RecipientContextPill(snapshot = snapshot)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = headline,
                modifier = Modifier.semantics { heading() },
                color = Ink,
                style = MaterialTheme.typography.headlineLarge,
            )
            Text(
                text = supporting,
                color = InkMuted,
                style = MaterialTheme.typography.bodyLarge,
            )
        }

        when (state) {
            DiscoveryUiState.Cancelled -> RecoveryVisual(
                icon = Icons.Rounded.Refresh,
                label = "Your clues are saved",
                containerColor = BlueSurface,
                contentColor = BrandBlue,
            )

            is DiscoveryUiState.Error -> RecoveryVisual(
                icon = Icons.Rounded.Refresh,
                label = "Safe to retry",
                containerColor = ErrorSoft,
                contentColor = ErrorRed,
            )

            else -> DiscoveryTraceScene(
                snapshot = snapshot,
                stageIndex = stageIndex,
                running = state is DiscoveryUiState.Running || state == DiscoveryUiState.Idle,
                ready = state is DiscoveryUiState.ReadyForRanking,
            )
        }

        DiscoveryStageRail(
            state = state,
            modifier = Modifier.fillMaxWidth(),
        )

        NoPurchaseStatus()
    }
}

@Composable
private fun RecipientContextPill(snapshot: HomeSnapshot) {
    Surface(
        color = SurfaceWhite,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, OutlineCool),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RecipientAvatar(
                initials = snapshot.recipient.initials,
                photoUri = snapshot.recipient.photoUri,
                size = 44.dp,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = snapshot.recipient.displayName,
                    color = Ink,
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    text = "${snapshot.occasion.kind.displayName} · ${snapshot.daysUntil} days",
                    color = InkMuted,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Surface(
                color = LavenderSurface,
                contentColor = BrandIndigoPressed,
                shape = CircleShape,
            ) {
                Text(
                    text = snapshot.occasion.budget.formatted(),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
    }
}

@Composable
private fun DiscoveryTraceScene(
    snapshot: HomeSnapshot,
    stageIndex: Int,
    running: Boolean,
    ready: Boolean,
) {
    val motionEnabled = rememberWishTraceMotionEnabled()
    val progress by animateFloatAsState(
        targetValue = stageIndex / DiscoveryStage.entries.size.toFloat(),
        animationSpec = if (motionEnabled) tween(460) else tween(0),
        label = "clue convergence",
    )
    val transition = rememberInfiniteTransition(label = "discovery float")
    val floatOffset by transition.animateFloat(
        initialValue = -4f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(1_700),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "gift float",
    )
    val resolvedFloat = if (motionEnabled && running) floatOffset else 0f

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(285.dp)
            .semantics {
                contentDescription = when {
                    ready -> "Recipient clues have converged around a prepared gift decision."
                    else -> "Recipient clues are moving into the gift discovery trace."
                }
                liveRegion = LiveRegionMode.Polite
            },
        color = LavenderSurface,
        shape = RoundedCornerShape(30.dp),
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    color = SurfaceWhite.copy(alpha = 0.76f),
                    radius = size.minDimension * 0.29f,
                )
                drawCircle(
                    color = BrandIndigo.copy(alpha = 0.12f),
                    radius = size.minDimension * 0.35f,
                    style = Stroke(width = 2.dp.toPx()),
                )
                drawArc(
                    color = if (ready) Success else BrandIndigo,
                    startAngle = -90f,
                    sweepAngle = 360f * progress.coerceIn(0.04f, 1f),
                    useCenter = false,
                    topLeft = androidx.compose.ui.geometry.Offset(
                        x = center.x - size.minDimension * 0.35f,
                        y = center.y - size.minDimension * 0.35f,
                    ),
                    size = androidx.compose.ui.geometry.Size(
                        width = size.minDimension * 0.7f,
                        height = size.minDimension * 0.7f,
                    ),
                    style = Stroke(
                        width = 4.dp.toPx(),
                        cap = StrokeCap.Round,
                    ),
                )
                listOf(
                    androidx.compose.ui.geometry.Offset(size.width * 0.18f, size.height * 0.28f),
                    androidx.compose.ui.geometry.Offset(size.width * 0.81f, size.height * 0.23f),
                    androidx.compose.ui.geometry.Offset(size.width * 0.16f, size.height * 0.76f),
                    androidx.compose.ui.geometry.Offset(size.width * 0.84f, size.height * 0.74f),
                ).forEachIndexed { index, point ->
                    drawCircle(
                        color = if (index < stageIndex) BrandBlue else OutlineCool,
                        radius = if (index < stageIndex) 5.dp.toPx() else 3.dp.toPx(),
                        center = point,
                    )
                }
            }

            ClueBubble(
                icon = Icons.Rounded.Lightbulb,
                text = snapshot.recipient.interests.firstOrNull() ?: "Their interests",
                progress = progress,
                startX = -106f,
                startY = -84f,
                endX = -74f,
                endY = -56f,
            )
            ClueBubble(
                icon = Icons.Rounded.AccountBalanceWallet,
                text = snapshot.occasion.budget.formatted(),
                progress = progress,
                startX = 104f,
                startY = -78f,
                endX = 74f,
                endY = -54f,
            )
            ClueBubble(
                icon = Icons.Rounded.CalendarMonth,
                text = "${snapshot.daysUntil} days",
                progress = progress,
                startX = -108f,
                startY = 82f,
                endX = -76f,
                endY = 54f,
            )
            ClueBubble(
                icon = Icons.Rounded.Shield,
                text = snapshot.recipient.dislikes.firstOrNull() ?: "No exclusions",
                progress = progress,
                startX = 104f,
                startY = 84f,
                endX = 74f,
                endY = 56f,
            )

            DimensionalAsset(
                drawableRes = R.drawable.wishtrace_gift_3d,
                description = null,
                modifier = Modifier
                    .size(if (ready) 144.dp else 134.dp)
                    .offset(y = resolvedFloat.dp),
            )

            if (running && !ready) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 18.dp),
                    color = SurfaceWhite.copy(alpha = 0.94f),
                    contentColor = BrandIndigoPressed,
                    shape = CircleShape,
                    shadowElevation = 3.dp,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 13.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(15.dp),
                            strokeWidth = 2.dp,
                        )
                        Text(
                            text = "Following the clues",
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                }
            }

            if (ready) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 18.dp),
                    color = SuccessSurface,
                    contentColor = Success,
                    shape = CircleShape,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 13.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text = "Context checked",
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ClueBubble(
    icon: ImageVector,
    text: String,
    progress: Float,
    startX: Float,
    startY: Float,
    endX: Float,
    endY: Float,
) {
    val x = startX + ((endX - startX) * progress)
    val y = startY + ((endY - startY) * progress)
    Surface(
        modifier = Modifier.offset(x = x.dp, y = y.dp),
        color = SurfaceWhite,
        contentColor = BrandIndigoPressed,
        shape = CircleShape,
        border = BorderStroke(1.dp, OutlineCool.copy(alpha = 0.75f)),
        shadowElevation = 3.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(17.dp),
            )
            Text(
                text = text,
                maxLines = 1,
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

@Composable
private fun DiscoveryStageRail(
    state: DiscoveryUiState,
    modifier: Modifier = Modifier,
) {
    val activeIndex = when (state) {
        is DiscoveryUiState.Running -> DiscoveryStage.entries.indexOf(state.activeStage)
        is DiscoveryUiState.ReadyForRanking -> DiscoveryStage.entries.lastIndex
        else -> 0
    }
    val completeCount = when (state) {
        is DiscoveryUiState.Running -> state.completedStages.size
        is DiscoveryUiState.ReadyForRanking -> DiscoveryStage.entries.size
        else -> 0
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        DiscoveryStage.entries.forEachIndexed { index, stage ->
            val complete = index < completeCount
            val active = index == activeIndex && state is DiscoveryUiState.Running
            StageTile(
                icon = stage.icon(),
                label = stage.label(),
                complete = complete,
                active = active,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun StageTile(
    icon: ImageVector,
    label: String,
    complete: Boolean,
    active: Boolean,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color = when {
            complete -> SuccessSurface
            active -> LavenderSurface
            else -> SurfaceWhite
        },
        contentColor = when {
            complete -> Success
            active -> BrandIndigoPressed
            else -> InkMuted
        },
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(
            width = 1.dp,
            color = if (active) BrandIndigo.copy(alpha = 0.32f) else OutlineCool,
        ),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 11.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                imageVector = if (complete) Icons.Rounded.Check else icon,
                contentDescription = null,
                modifier = Modifier.size(21.dp),
            )
            Text(
                text = label,
                maxLines = 1,
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

@Composable
private fun NoPurchaseStatus() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Rounded.Shield,
            contentDescription = null,
            tint = InkMuted,
            modifier = Modifier.size(17.dp),
        )
        Spacer(modifier = Modifier.width(7.dp))
        Text(
            text = "No purchase has started",
            color = InkMuted,
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

@Composable
private fun RecoveryVisual(
    icon: ImageVector,
    label: String,
    containerColor: androidx.compose.ui.graphics.Color,
    contentColor: androidx.compose.ui.graphics.Color,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(330.dp),
        color = containerColor,
        contentColor = contentColor,
        shape = RoundedCornerShape(30.dp),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Surface(
                modifier = Modifier.size(92.dp),
                color = SurfaceWhite,
                contentColor = contentColor,
                shape = CircleShape,
                shadowElevation = 4.dp,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(42.dp),
                    )
                }
            }
            Text(
                text = label,
                modifier = Modifier.padding(top = 18.dp),
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}

@Composable
private fun DiscoveryActionBar(
    state: DiscoveryUiState,
    recipientName: String,
    onCancel: () -> Unit,
    onRetry: () -> Unit,
    onContinue: () -> Unit,
    onBack: () -> Unit,
) {
    Surface(
        color = SurfaceWhite,
        shadowElevation = 8.dp,
    ) {
        Column(
            modifier = Modifier
                .navigationBarsPadding()
                .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 14.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            when (state) {
                DiscoveryUiState.Idle,
                is DiscoveryUiState.Running,
                -> SecondaryAction(
                    text = "Cancel",
                    onClick = onCancel,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(WishTraceTestTags.CancelDiscovery),
                    enabled = state is DiscoveryUiState.Running,
                )

                is DiscoveryUiState.ReadyForRanking -> PrimaryAction(
                    text = "See the decision",
                    onClick = onContinue,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(WishTraceTestTags.ContinueDiscovery),
                )

                DiscoveryUiState.Cancelled,
                is DiscoveryUiState.Error,
                -> {
                    PrimaryAction(
                        text = "Try again",
                        onClick = onRetry,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(WishTraceTestTags.RetryDiscovery),
                        trailingContent = {
                            Icon(
                                imageVector = Icons.Rounded.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                        },
                    )
                    SecondaryAction(
                        text = "Back to $recipientName",
                        onClick = onBack,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Composable
private fun DiscoveryUnavailable(
    title: String,
    message: String,
    showProgress: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        if (showProgress) {
            CircularProgressIndicator(modifier = Modifier.padding(bottom = 18.dp))
        }
        Text(
            text = title,
            modifier = Modifier.semantics { heading() },
            color = Ink,
            style = MaterialTheme.typography.headlineMedium,
        )
        Text(
            text = message,
            modifier = Modifier.padding(top = 8.dp),
            color = InkMuted,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

private fun DiscoveryStage.label(): String = when (this) {
    DiscoveryStage.CHECKING_CATALOG -> "Products"
    DiscoveryStage.APPLYING_BUDGET -> "Budget"
    DiscoveryStage.CHECKING_FULFILLMENT -> "Delivery"
    DiscoveryStage.PREPARING_RANKING -> "Match"
}

private fun DiscoveryStage.icon(): ImageVector = when (this) {
    DiscoveryStage.CHECKING_CATALOG -> Icons.Rounded.ShoppingBag
    DiscoveryStage.APPLYING_BUDGET -> Icons.Rounded.AccountBalanceWallet
    DiscoveryStage.CHECKING_FULFILLMENT -> Icons.Rounded.LocalShipping
    DiscoveryStage.PREPARING_RANKING -> Icons.Rounded.Favorite
}

private fun DiscoveryStage.detail(snapshot: HomeSnapshot): String = when (this) {
    DiscoveryStage.CHECKING_CATALOG -> "Looking only at supplied product records."
    DiscoveryStage.APPLYING_BUDGET -> "Keeping the total within ${snapshot.occasion.budget.formatted()}."
    DiscoveryStage.CHECKING_FULFILLMENT -> "Reviewing known variant and arrival facts."
    DiscoveryStage.PREPARING_RANKING -> "Linking eligible choices to ${snapshot.recipient.displayName}’s clues."
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun DiscoveryProgressPreview() {
    WishTraceTheme {
        GiftDiscoveryScreen(
            homeState = HomeUiState.Content(PreviewFixtures.homeSnapshot()),
            state = DiscoveryUiState.Running(
                activeStage = DiscoveryStage.CHECKING_FULFILLMENT,
                completedStages = listOf(
                    DiscoveryStage.CHECKING_CATALOG,
                    DiscoveryStage.APPLYING_BUDGET,
                ),
            ),
            onBack = {},
            onStart = {},
            onCancel = {},
            onContinue = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun DiscoveryReadyPreview() {
    WishTraceTheme {
        GiftDiscoveryScreen(
            homeState = HomeUiState.Content(PreviewFixtures.homeSnapshot()),
            state = DiscoveryUiState.ReadyForRanking(
                eligibleCandidateIds = listOf("alpha", "bravo"),
                sourceMode = com.wishtrace.app.domain.SourceMode.LIVE,
            ),
            onBack = {},
            onStart = {},
            onCancel = {},
            onContinue = {},
        )
    }
}
