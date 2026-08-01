package com.wishtrace.app.ui.screens.people

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.wishtrace.app.R
import com.wishtrace.app.domain.HomeSnapshot
import com.wishtrace.app.ui.HomeUiState
import com.wishtrace.app.ui.components.DimensionalAsset
import com.wishtrace.app.ui.components.InterestChip
import com.wishtrace.app.ui.components.PrimaryAction
import com.wishtrace.app.ui.components.RecipientAvatar
import com.wishtrace.app.ui.theme.BrandBlue
import com.wishtrace.app.ui.theme.BrandIndigo
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun PeopleScreen(
    state: HomeUiState,
    onRetry: () -> Unit,
    onOpenRecipient: () -> Unit,
    onAddPerson: () -> Unit,
    onEditPerson: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (state) {
        HomeUiState.Loading -> LoadingState(modifier)
        HomeUiState.Empty -> EmptyPeople(onAddPerson, modifier)
        is HomeUiState.Error -> ErrorState(state.message, onRetry, modifier)
        is HomeUiState.Content -> PeopleContent(
            snapshot = state.snapshot,
            onOpenRecipient = onOpenRecipient,
            onEditPerson = onEditPerson,
            modifier = modifier,
        )
    }
}

@Composable
private fun PeopleContent(
    snapshot: HomeSnapshot,
    onOpenRecipient: () -> Unit,
    onEditPerson: () -> Unit,
    modifier: Modifier,
) {
    val recipient = snapshot.recipient
    val occasion = snapshot.occasion
    val formatter = DateTimeFormatter.ofPattern("MMM d", Locale.US)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = PaddingValues(
            start = 20.dp,
            end = 20.dp,
            top = 20.dp,
            bottom = 32.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "People",
                    modifier = Modifier.semantics { heading() },
                    style = MaterialTheme.typography.headlineLarge,
                )
                TextButton(onClick = onEditPerson) {
                    Text(text = "Edit")
                }
            }
        }
        item {
            Surface(
                onClick = onOpenRecipient,
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                shape = MaterialTheme.shapes.large,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            ) {
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
                            size = 58.dp,
                        )
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            Text(
                                text = recipient.displayName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = recipient.relationship,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                        Icon(
                            imageVector = Icons.Rounded.ArrowForward,
                            contentDescription = "Open ${recipient.displayName}'s profile",
                            tint = BrandIndigo,
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CalendarMonth,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = BrandBlue,
                        )
                        Text(
                            text = "${occasion.kind.displayName} · ${occasion.localDate.format(formatter)}",
                            style = MaterialTheme.typography.labelMedium,
                        )
                        Text(
                            text = "· ${snapshot.daysUntil} days",
                            color = BrandIndigo,
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        recipient.interests.take(2).forEach { interest ->
                            InterestChip(text = interest)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyPeople(onAddPerson: () -> Unit, modifier: Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        DimensionalAsset(
            drawableRes = R.drawable.wishtrace_gift_3d,
            description = "A wrapped purple gift",
            modifier = Modifier.size(152.dp),
        )
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = "Who are you gifting?",
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.headlineMedium,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Start with one person and their next occasion.",
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
private fun LoadingState(modifier: Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ErrorState(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = "People could not load.", style = MaterialTheme.typography.headlineMedium)
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
