package com.wishtrace.app.ui.screens.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Lightbulb
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
import com.wishtrace.app.data.ControlledFixtures
import com.wishtrace.app.domain.HomeSnapshot
import com.wishtrace.app.ui.HomeUiState
import com.wishtrace.app.ui.WishTraceTestTags
import com.wishtrace.app.ui.components.EditorialCard
import com.wishtrace.app.ui.components.InterestChip
import com.wishtrace.app.ui.components.PrimaryAction
import com.wishtrace.app.ui.components.RecipientAvatar
import com.wishtrace.app.ui.components.StaggeredEntrance
import com.wishtrace.app.ui.components.WishTraceWordmark
import com.wishtrace.app.ui.theme.BlueSurface
import com.wishtrace.app.ui.theme.BrandBlue
import com.wishtrace.app.ui.theme.BrandIndigo
import com.wishtrace.app.ui.theme.LavenderSurface
import com.wishtrace.app.ui.theme.WishTraceTheme
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun HomeScreen(
    state: HomeUiState,
    onRetry: () -> Unit,
    onFindGift: () -> Unit,
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
                onFindGift = onFindGift,
                onReviewRecipient = onReviewRecipient,
            )
        }
    }
}

@Composable
private fun HomeContent(
    snapshot: HomeSnapshot,
    onFindGift: () -> Unit,
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
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item {
            StaggeredEntrance(index = 0) {
                WishTraceWordmark(markSize = 30.dp)
            }
        }
        item {
            StaggeredEntrance(index = 1) {
                Text(
                    text = "Hi, Talia",
                    modifier = Modifier.semantics { heading() },
                    style = MaterialTheme.typography.headlineLarge,
                )
            }
        }
        item {
            StaggeredEntrance(index = 2) {
                EditorialCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            RecipientAvatar(
                                initials = recipient.initials,
                                photoUri = recipient.photoUri,
                                size = 56.dp,
                            )
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(2.dp),
                            ) {
                                Text(
                                    text = "${recipient.displayName}'s ${occasion.kind.displayName}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                )
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.CalendarMonth,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = BrandBlue,
                                    )
                                    Text(
                                        text = occasion.localDate.format(dateFormatter),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                }
                            }
                            Surface(
                                color = LavenderSurface,
                                contentColor = BrandIndigo,
                                shape = RoundedCornerShape(16.dp),
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    Text(
                                        text = snapshot.daysUntil.toString(),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    Text(
                                        text = "days",
                                        style = MaterialTheme.typography.labelSmall,
                                    )
                                }
                            }
                        }

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(0.dp),
                        ) {
                            items(recipient.interests.take(3).size) { index ->
                                InterestChip(text = recipient.interests[index])
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "Gift budget",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Text(
                                text = occasion.budget.formatted(Locale.US),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                            )
                        }

                        PrimaryAction(
                            text = "Find a gift for ${recipient.displayName}",
                            onClick = onFindGift,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag(WishTraceTestTags.FindGift),
                            trailingContent = {
                                Icon(
                                    imageVector = Icons.Rounded.ArrowForward,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                            },
                        )
                    }
                }
            }
        }
        if (recipient.hints.isNotEmpty()) {
            item {
                StaggeredEntrance(index = 3) {
                Surface(
                    onClick = onReviewRecipient,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(WishTraceTestTags.ReviewRecipient),
                    color = BlueSurface,
                    shape = RoundedCornerShape(20.dp),
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
                                text = "Saved clue",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = recipient.hints.first().text,
                                maxLines = 2,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                        Icon(
                            imageVector = Icons.Rounded.ArrowForward,
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
            state = HomeUiState.Content(ControlledFixtures.homeSnapshot()),
            onRetry = {},
            onFindGift = {},
            onReviewRecipient = {},
        )
    }
}
