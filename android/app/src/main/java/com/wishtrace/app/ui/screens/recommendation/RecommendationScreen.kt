package com.wishtrace.app.ui.screens.recommendation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.LocalShipping
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wishtrace.app.R
import com.wishtrace.app.data.PreviewFixtures
import com.wishtrace.app.domain.CandidateRejection
import com.wishtrace.app.domain.HomeSnapshot
import com.wishtrace.app.domain.ProductCandidate
import com.wishtrace.app.domain.RankedDecision
import com.wishtrace.app.domain.SourceMode
import com.wishtrace.app.ui.WishTraceTestTags
import com.wishtrace.app.ui.components.DimensionalAsset
import com.wishtrace.app.ui.components.PrimaryAction
import com.wishtrace.app.ui.components.RecipientAvatar
import com.wishtrace.app.ui.components.ScreenTopBar
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
import java.time.ZoneId
import java.time.format.DateTimeFormatter

sealed interface RecommendationUiState {
    data object Loading : RecommendationUiState
    data object SourceNeeded : RecommendationUiState
    data object Empty : RecommendationUiState
    data class Error(val message: String) : RecommendationUiState

    data class Content(
        val candidates: List<ProductCandidate>,
        val decision: RankedDecision,
    ) : RecommendationUiState
}

@Composable
fun RecommendationScreen(
    snapshot: HomeSnapshot,
    state: RecommendationUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onSelect: (String) -> Unit,
    onWriteMessage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val content = state as? RecommendationUiState.Content
    val recommendedCandidateId = content?.decision?.selectedCandidateId
    var chosenCandidateId by rememberSaveable(recommendedCandidateId) {
        mutableStateOf<String?>(recommendedCandidateId)
    }
    val selectedCandidate = content
        ?.candidates
        ?.firstOrNull { it.id == chosenCandidateId }

    Scaffold(
        modifier = modifier.testTag(WishTraceTestTags.RecommendationScreen),
        containerColor = Canvas,
        topBar = {
            ScreenTopBar(
                title = if (state is RecommendationUiState.Content) {
                    "Best fits for ${snapshot.recipient.displayName}"
                } else {
                    "Finding a gift"
                },
                onBack = onBack,
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp),
            )
        },
        bottomBar = {
            RecommendationActionBar(
                state = state,
                selectedCandidateId = selectedCandidate?.id,
                recipientName = snapshot.recipient.displayName,
                onBack = onBack,
                onRetry = onRetry,
                onSelect = onSelect,
                onWriteMessage = onWriteMessage,
            )
        },
    ) { innerPadding ->
        when (state) {
            RecommendationUiState.Loading -> DecisionLoading(
                modifier = Modifier.padding(innerPadding),
            )

            RecommendationUiState.SourceNeeded -> SourceNeededContent(
                snapshot = snapshot,
                modifier = Modifier.padding(innerPadding),
            )

            RecommendationUiState.Empty -> DecisionEmpty(
                modifier = Modifier.padding(innerPadding),
            )

            is RecommendationUiState.Error -> DecisionError(
                message = state.message,
                modifier = Modifier.padding(innerPadding),
            )

            is RecommendationUiState.Content -> {
                if (selectedCandidate == null) {
                    DecisionError(
                        message = "The ranked candidate is missing from the sourced shortlist.",
                        modifier = Modifier.padding(innerPadding),
                    )
                } else {
                    DecisionContent(
                        snapshot = snapshot,
                        selected = selectedCandidate,
                        state = state,
                        onChoose = { chosenCandidateId = it },
                        modifier = Modifier.padding(innerPadding),
                    )
                }
            }
        }
    }
}

@Composable
private fun SourceNeededContent(
    snapshot: HomeSnapshot,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        DecisionRecipientPill(snapshot)
        SourceNeededVisual()
        Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Text(
                text = "One real detail is missing.",
                modifier = Modifier.semantics { heading() },
                color = Ink,
                style = MaterialTheme.typography.headlineLarge,
            )
            Text(
                text = "Connect a verified product source to see a recommendation you can trust.",
                color = InkMuted,
                style = MaterialTheme.typography.bodyLarge,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            ReadyFact(
                icon = Icons.Rounded.AccountBalanceWallet,
                label = "Budget",
                value = snapshot.occasion.budget.formatted(),
                modifier = Modifier.weight(1f),
            )
            ReadyFact(
                icon = Icons.Rounded.CalendarMonth,
                label = "Timing",
                value = "${snapshot.daysUntil} days",
                modifier = Modifier.weight(1f),
            )
            ReadyFact(
                icon = Icons.Rounded.Lightbulb,
                label = "Clues",
                value = snapshot.recipient.interests.size.toString(),
                modifier = Modifier.weight(1f),
            )
        }
        Surface(
            color = BlueSurface,
            contentColor = BrandIndigoPressed,
            shape = RoundedCornerShape(18.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Shield,
                    contentDescription = null,
                    modifier = Modifier.size(21.dp),
                )
                Text(
                    text = "No product, price or delivery promise is guessed.",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
private fun SourceNeededVisual() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(250.dp),
        color = LavenderSurface,
        shape = RoundedCornerShape(30.dp),
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                modifier = Modifier
                    .size(width = 154.dp, height = 178.dp)
                    .offset(x = (-36).dp)
                    .graphicsLayer { rotationZ = -7f },
                color = SurfaceWhite.copy(alpha = 0.72f),
                shape = RoundedCornerShape(28.dp),
                border = BorderStroke(1.dp, OutlineCool),
            ) {}
            Surface(
                modifier = Modifier
                    .size(width = 154.dp, height = 178.dp)
                    .offset(x = 36.dp)
                    .graphicsLayer { rotationZ = 7f },
                color = BlueSurface.copy(alpha = 0.92f),
                shape = RoundedCornerShape(28.dp),
                border = BorderStroke(1.dp, OutlineCool),
            ) {}
            Surface(
                modifier = Modifier.size(158.dp),
                color = SurfaceWhite,
                shape = CircleShape,
                shadowElevation = 5.dp,
            ) {
                DimensionalAsset(
                    drawableRes = R.drawable.wishtrace_gift_3d,
                    description = null,
                    modifier = Modifier.padding(13.dp),
                )
            }
            SourceOrbitIcon(
                icon = Icons.Rounded.Search,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 32.dp, end = 30.dp)
                    .graphicsLayer { rotationZ = 7f },
            )
            SourceOrbitIcon(
                icon = Icons.Rounded.Shield,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 28.dp, bottom = 34.dp)
                    .graphicsLayer { rotationZ = -6f },
            )
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 26.dp, bottom = 28.dp),
                color = SurfaceWhite,
                contentColor = BrandIndigoPressed,
                shape = CircleShape,
                shadowElevation = 3.dp,
            ) {
                Text(
                    text = "SOURCE",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
    }
}

@Composable
private fun SourceOrbitIcon(
    icon: ImageVector,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.size(56.dp),
        color = SurfaceWhite,
        contentColor = BrandIndigo,
        shape = RoundedCornerShape(18.dp),
        shadowElevation = 4.dp,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(25.dp),
            )
        }
    }
}

@Composable
private fun ReadyFact(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color = SurfaceWhite,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, OutlineCool),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = BrandBlue,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = value,
                color = Ink,
                style = MaterialTheme.typography.labelMedium,
            )
            Text(
                text = label,
                color = InkMuted,
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

@Composable
private fun DecisionRecipientPill(snapshot: HomeSnapshot) {
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
                color = SuccessSurface,
                contentColor = Success,
                shape = CircleShape,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = "Context",
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
        }
    }
}

@Composable
private fun DecisionContent(
    snapshot: HomeSnapshot,
    selected: ProductCandidate,
    state: RecommendationUiState.Content,
    onChoose: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val rationale = state.decision.rationales.first {
        it.candidateId == selected.id
    }
    val rankedIds = listOf(state.decision.selectedCandidateId) +
        state.decision.alternativeCandidateIds
    val alternatives = rankedIds.filterNot { it == selected.id }.mapNotNull { id ->
        state.candidates.firstOrNull { it.id == id }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 30.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        DecisionRecipientPill(snapshot)
        RecommendationBento(
            candidate = selected,
            rationale = rationale.reason,
            isPrimary = selected.id == state.decision.selectedCandidateId,
        )

        if (alternatives.isNotEmpty()) {
            Text(
                text = "OTHER GOOD FITS",
                color = BrandIndigo,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                alternatives.forEachIndexed { index, candidate ->
                    AlternativeCard(
                        candidate = candidate,
                        containerColor = if (index == 0) LavenderSurface else BlueSurface,
                        onChoose = { onChoose(candidate.id) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        if (state.decision.rejections.isNotEmpty()) {
            RejectionsSection(state.decision.rejections)
        }
    }
}

@Composable
private fun RecommendationBento(
    candidate: ProductCandidate,
    rationale: String,
    isPrimary: Boolean,
) {
    val formatter = DateTimeFormatter.ofPattern("MMM d, h:mm a")
    val recorded = formatter.format(
        candidate.sourceTimestamp.atZone(ZoneId.systemDefault()),
    )
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(238.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Surface(
                modifier = Modifier
                    .weight(1.45f)
                    .fillMaxHeight(),
                color = LavenderSurface,
                contentColor = Ink,
                shape = RoundedCornerShape(28.dp),
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Surface(
                            modifier = Modifier.size(48.dp),
                            color = SurfaceWhite,
                            contentColor = BrandIndigo,
                            shape = RoundedCornerShape(16.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.ShoppingBag,
                                    contentDescription = null,
                                    modifier = Modifier.size(25.dp),
                                )
                            }
                        }
                        Text(
                            text = if (isPrimary) "TOP MATCH" else "YOUR PICK",
                            color = BrandIndigo,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(
                            text = candidate.title,
                            modifier = Modifier.semantics { heading() },
                            color = Ink,
                            style = MaterialTheme.typography.headlineMedium,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = candidate.merchantName,
                            color = InkMuted,
                            style = MaterialTheme.typography.labelLarge,
                            maxLines = 1,
                        )
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
                        Text("PRICE", style = MaterialTheme.typography.labelSmall)
                        Text(
                            text = candidate.currentPrice.formatted(),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                        )
                        Text("USD", style = MaterialTheme.typography.labelSmall)
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
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = null,
                            tint = BrandBlue,
                        )
                        Text(
                            text = candidate.sourceMode.displayName().uppercase(),
                            color = BrandBlue,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = recorded,
                            color = InkMuted,
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                        )
                    }
                }
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Ink,
            contentColor = SurfaceWhite,
            shape = RoundedCornerShape(24.dp),
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.Lightbulb, contentDescription = null, tint = BrandBlue)
                Column(modifier = Modifier.weight(1f)) {
                    Text("WHY IT FITS", style = MaterialTheme.typography.labelSmall)
                    Text(
                        text = rationale,
                        color = SurfaceWhite,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = SurfaceWhite,
            contentColor = Ink,
            shape = RoundedCornerShape(22.dp),
            border = BorderStroke(1.dp, OutlineCool),
        ) {
            Row(
                modifier = Modifier.padding(15.dp),
                horizontalArrangement = Arrangement.spacedBy(11.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.LocalShipping, contentDescription = null, tint = BrandBlue)
                Text(
                    text = candidate.supportedDeliveryFact ?: "Delivery not confirmed",
                    modifier = Modifier.weight(1f),
                    color = Ink,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                )
            }
        }
    }
}

@Composable
private fun AlternativeCard(
    candidate: ProductCandidate,
    containerColor: androidx.compose.ui.graphics.Color,
    onChoose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onChoose,
        modifier = modifier.heightIn(min = 154.dp),
        color = containerColor,
        shape = RoundedCornerShape(24.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(15.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    modifier = Modifier.size(40.dp),
                    color = SurfaceWhite,
                    contentColor = BrandIndigoPressed,
                    shape = RoundedCornerShape(13.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.ShoppingBag,
                            contentDescription = null,
                            modifier = Modifier.size(21.dp),
                        )
                    }
                }
                Text(
                    text = candidate.currentPrice.formatted(),
                    color = Ink,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = candidate.title,
                    color = Ink,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = candidate.merchantName,
                    color = InkMuted,
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun RejectionsSection(rejections: List<CandidateRejection>) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Surface(
        color = SurfaceWhite,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, OutlineCool),
    ) {
        Column {
            TextButton(
                onClick = { expanded = !expanded },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = "Ruled out",
                    modifier = Modifier.weight(1f),
                    color = Ink,
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    text = rejections.size.toString(),
                    color = InkMuted,
                    style = MaterialTheme.typography.labelMedium,
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = if (expanded) {
                        Icons.Rounded.ExpandLess
                    } else {
                        Icons.Rounded.ExpandMore
                    },
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = InkMuted,
                )
            }
            if (expanded) {
                rejections.take(3).forEach { rejection ->
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                        horizontalArrangement = Arrangement.spacedBy(9.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = null,
                            tint = ErrorRed,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text = rejection.explanation,
                            color = InkMuted,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun DecisionLoading(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator()
        Text(
            text = "Grounding the decision",
            modifier = Modifier.padding(top = 16.dp),
            color = Ink,
            style = MaterialTheme.typography.titleMedium,
        )
    }
}

@Composable
private fun DecisionEmpty(modifier: Modifier = Modifier) {
    DecisionState(
        icon = Icons.Rounded.Search,
        title = "No eligible gifts yet",
        message = "Adjust the budget or timing, then search again.",
        modifier = modifier,
    )
}

@Composable
private fun DecisionError(
    message: String,
    modifier: Modifier = Modifier,
) {
    DecisionState(
        icon = Icons.Rounded.Refresh,
        title = "The decision needs another try",
        message = message,
        modifier = modifier,
        error = true,
    )
}

@Composable
private fun DecisionState(
    icon: ImageVector,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    error: Boolean = false,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Surface(
            modifier = Modifier.size(92.dp),
            color = if (error) ErrorSoft else BlueSurface,
            contentColor = if (error) ErrorRed else BrandBlue,
            shape = CircleShape,
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
            text = title,
            modifier = Modifier
                .padding(top = 20.dp)
                .semantics { heading() },
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

@Composable
private fun RecommendationActionBar(
    state: RecommendationUiState,
    selectedCandidateId: String?,
    recipientName: String,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onSelect: (String) -> Unit,
    onWriteMessage: () -> Unit,
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
                RecommendationUiState.Loading -> Unit

                RecommendationUiState.SourceNeeded -> {
                    PrimaryAction(
                        text = "Write $recipientName’s note",
                        onClick = onWriteMessage,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(WishTraceTestTags.WriteMessage),
                    )
                }

                RecommendationUiState.Empty,
                is RecommendationUiState.Error,
                -> {
                    PrimaryAction(
                        text = "Try again",
                        onClick = onRetry,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                is RecommendationUiState.Content -> {
                    PrimaryAction(
                        text = "Choose this gift",
                        onClick = { selectedCandidateId?.let(onSelect) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = selectedCandidateId != null,
                    )
                }
            }
        }
    }
}

private fun SourceMode.displayName(): String = when (this) {
    SourceMode.LIVE -> "Live"
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun SourceNeededPreview() {
    WishTraceTheme {
        RecommendationScreen(
            snapshot = PreviewFixtures.homeSnapshot(),
            state = RecommendationUiState.SourceNeeded,
            onBack = {},
            onRetry = {},
            onSelect = {},
            onWriteMessage = {},
        )
    }
}
