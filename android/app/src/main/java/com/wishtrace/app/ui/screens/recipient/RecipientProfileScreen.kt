package com.wishtrace.app.ui.screens.recipient

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Shield
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wishtrace.app.data.PreviewFixtures
import com.wishtrace.app.domain.HomeSnapshot
import com.wishtrace.app.ui.HomeUiState
import com.wishtrace.app.ui.WishTraceTestTags
import com.wishtrace.app.ui.components.PrimaryAction
import com.wishtrace.app.ui.components.RecipientAvatar
import com.wishtrace.app.ui.components.ScreenTopBar
import com.wishtrace.app.ui.theme.BlueSurface
import com.wishtrace.app.ui.theme.BrandBlue
import com.wishtrace.app.ui.theme.BrandIndigo
import com.wishtrace.app.ui.theme.Ink
import com.wishtrace.app.ui.theme.InkMuted
import com.wishtrace.app.ui.theme.LavenderSurface
import com.wishtrace.app.ui.theme.SurfaceWhite
import com.wishtrace.app.ui.theme.WishTraceTheme
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun RecipientProfileScreen(
    state: HomeUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onFindGift: () -> Unit,
    onEdit: () -> Unit = {},
    onChooseContact: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val snapshot = (state as? HomeUiState.Content)?.snapshot

    Scaffold(
        modifier = modifier.testTag(WishTraceTestTags.RecipientScreen),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            ScreenTopBar(
                title = snapshot?.recipient?.displayName ?: "Person",
                onBack = onBack,
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp),
                action = {
                    if (snapshot != null) {
                        TextButton(onClick = onEdit) {
                            Text(text = "Edit")
                        }
                    }
                },
            )
        },
        bottomBar = {
            if (snapshot != null) {
                Surface(color = MaterialTheme.colorScheme.background) {
                    PrimaryAction(
                        text = "Find a gift",
                        onClick = onFindGift,
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 14.dp)
                            .testTag(WishTraceTestTags.ProfileFindGift),
                    )
                }
            }
        },
    ) { innerPadding ->
        when (state) {
            HomeUiState.Loading -> ProfileLoading(Modifier.padding(innerPadding))
            HomeUiState.Empty -> ProfileUnavailable(
                title = "No person yet",
                actionLabel = "Go back",
                onAction = onBack,
                modifier = Modifier.padding(innerPadding),
            )

            is HomeUiState.Error -> ProfileUnavailable(
                title = "Profile unavailable",
                actionLabel = "Try again",
                onAction = onRetry,
                modifier = Modifier.padding(innerPadding),
            )

            is HomeUiState.Content -> ProfileContent(
                snapshot = state.snapshot,
                onChooseContact = onChooseContact,
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
}

@Composable
private fun ProfileContent(
    snapshot: HomeSnapshot,
    onChooseContact: () -> Unit,
    modifier: Modifier,
) {
    val recipient = snapshot.recipient
    val occasion = snapshot.occasion
    val shortDate = DateTimeFormatter.ofPattern("MMM d", Locale.US)
    var detailsExpanded by rememberSaveable { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 20.dp,
            end = 20.dp,
            top = 8.dp,
            bottom = 28.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(154.dp),
                color = LavenderSurface,
                contentColor = Ink,
                shape = RoundedCornerShape(28.dp),
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Box {
                        Surface(
                            onClick = onChooseContact,
                            color = SurfaceWhite,
                            shape = CircleShape,
                            modifier = Modifier.semantics {
                                contentDescription =
                                    "Choose ${recipient.displayName}'s contact photo"
                            },
                        ) {
                            RecipientAvatar(
                                initials = recipient.initials,
                                photoUri = recipient.photoUri,
                                size = 88.dp,
                            )
                        }
                        Surface(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(1.dp),
                            color = BrandIndigo,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            shape = CircleShape,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Edit,
                                contentDescription = null,
                                modifier = Modifier.padding(7.dp),
                            )
                        }
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "GIFT PROFILE",
                            color = BrandIndigo,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = recipient.displayName,
                            modifier = Modifier.semantics { heading() },
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = recipient.relationship,
                            color = InkMuted,
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
            }
        }
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = BlueSurface,
                shape = MaterialTheme.shapes.large,
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CalendarMonth,
                                contentDescription = null,
                                tint = BrandBlue,
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = occasion.kind.displayName,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(
                                    text = occasion.localDate.format(shortDate),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                        }
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            contentColor = BrandIndigo,
                            shape = CircleShape,
                        ) {
                            Text(
                                text = "${snapshot.daysUntil} days",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                style = MaterialTheme.typography.labelMedium,
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = "Budget",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            text = occasion.budget.formatted(Locale.US),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
        item {
            CompactSection(title = "Gift DNA") {
                if (recipient.interests.isEmpty()) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = BlueSurface,
                        contentColor = Ink,
                        shape = RoundedCornerShape(22.dp),
                    ) {
                        Text(
                            text = "No interests saved yet",
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(108.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        recipient.interests.take(2).forEachIndexed { index, interest ->
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                                color = if (index == 0) BlueSurface else LavenderSurface,
                                contentColor = Ink,
                                shape = RoundedCornerShape(22.dp),
                            ) {
                                Column(
                                    modifier = Modifier.padding(15.dp),
                                    verticalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Text(
                                        text = "LIKES",
                                        color = if (index == 0) BrandBlue else BrandIndigo,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    Text(
                                        text = interest,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 2,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = SurfaceWhite,
                contentColor = Ink,
                shape = RoundedCornerShape(22.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(
                        modifier = Modifier.size(42.dp),
                        color = BlueSurface,
                        contentColor = BrandBlue,
                        shape = CircleShape,
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.Shield,
                                contentDescription = null,
                            )
                        }
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "Avoid",
                            style = MaterialTheme.typography.labelSmall,
                            color = InkMuted,
                        )
                        Text(
                            text = recipient.dislikes.takeIf { it.isNotEmpty() }
                                ?.joinToString(" · ")
                                ?: "No exclusions saved",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                        )
                    }
                }
            }
        }
        if (recipient.hints.isNotEmpty()) {
            item {
                CompactSection(title = "Saved clue") {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = BlueSurface,
                    shape = MaterialTheme.shapes.large,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Lightbulb,
                            contentDescription = null,
                            tint = BrandBlue,
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Text(
                            text = recipient.hints.first().text,
                                style = MaterialTheme.typography.bodyLarge,
                            )
                            Text(
                                text = recipient.hints.first().sourceLabel,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                    }
                    }
                }
            }
        }
        item {
            TextButton(onClick = { detailsExpanded = !detailsExpanded }) {
                Text(text = if (detailsExpanded) "Fewer details" else "More details")
                Icon(
                    imageVector = if (detailsExpanded) {
                        Icons.Rounded.ExpandLess
                    } else {
                        Icons.Rounded.ExpandMore
                    },
                    contentDescription = null,
                )
            }
            if (detailsExpanded) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shape = MaterialTheme.shapes.medium,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = "Local time · ${occasion.timeZone.id}",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        occasion.requiredArrivalDate?.let {
                            Text(
                                text = "Needed by · ${it.format(shortDate)}",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactSection(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
        )
        content()
    }
}

@Composable
private fun ProfileLoading(modifier: Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ProfileUnavailable(
    title: String,
    actionLabel: String,
    onAction: () -> Unit,
    modifier: Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = title, style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(20.dp))
        PrimaryAction(
            text = actionLabel,
            onClick = onAction,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun RecipientProfilePreview() {
    WishTraceTheme {
        RecipientProfileScreen(
            state = HomeUiState.Content(PreviewFixtures.homeSnapshot()),
            onBack = {},
            onRetry = {},
            onFindGift = {},
        )
    }
}
