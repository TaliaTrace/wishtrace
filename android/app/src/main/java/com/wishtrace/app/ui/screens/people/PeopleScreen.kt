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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.People
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.wishtrace.app.R
import com.wishtrace.app.domain.Recipient
import com.wishtrace.app.ui.PeopleUiState
import com.wishtrace.app.ui.components.DimensionalAsset
import com.wishtrace.app.ui.components.PrimaryAction
import com.wishtrace.app.ui.components.RecipientAvatar
import com.wishtrace.app.ui.theme.BlueSurface
import com.wishtrace.app.ui.theme.BrandBlue
import com.wishtrace.app.ui.theme.BrandIndigo
import com.wishtrace.app.ui.theme.Ink
import com.wishtrace.app.ui.theme.InkMuted
import com.wishtrace.app.ui.theme.LavenderSurface
import com.wishtrace.app.ui.theme.OutlineCool
import com.wishtrace.app.ui.theme.SurfaceWhite

@Composable
fun PeopleScreen(
    state: PeopleUiState,
    onRetry: () -> Unit,
    onAddPerson: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (state) {
        PeopleUiState.Loading -> LoadingState(modifier)
        PeopleUiState.Empty -> EmptyPeople(onAddPerson, modifier)
        is PeopleUiState.Error -> ErrorState(state.message, onRetry, modifier)
        is PeopleUiState.Content -> PeopleContent(
            recipients = state.recipients,
            onAddPerson = onAddPerson,
            modifier = modifier,
        )
    }
}

@Composable
private fun PeopleContent(
    recipients: List<Recipient>,
    onAddPerson: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = PaddingValues(
            start = 20.dp,
            end = 20.dp,
            top = 18.dp,
            bottom = 32.dp,
        ),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "People",
                        modifier = Modifier.semantics { heading() },
                        color = Ink,
                        style = MaterialTheme.typography.headlineLarge,
                    )
                    Text(
                        text = "${recipients.size} remembered",
                        color = InkMuted,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                TextButton(
                    onClick = onAddPerson,
                    modifier = Modifier.height(48.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = null,
                        modifier = Modifier.size(19.dp),
                    )
                    Text("Add")
                }
            }
        }

        itemsIndexed(
            items = recipients,
            key = { _, recipient -> recipient.id },
        ) { index, recipient ->
            PersonTile(
                recipient = recipient,
                index = index,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        item(span = { GridItemSpan(maxLineSpan) }) {
            Surface(
                onClick = onAddPerson,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp),
                color = SurfaceWhite,
                contentColor = BrandIndigo,
                shape = RoundedCornerShape(22.dp),
                border = BorderStroke(1.dp, OutlineCool),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(11.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(
                        modifier = Modifier.size(40.dp),
                        color = LavenderSurface,
                        contentColor = BrandIndigo,
                        shape = CircleShape,
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = null,
                            )
                        }
                    }
                    Column {
                        Text(
                            text = "Remember someone else",
                            color = Ink,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "Add their next important moment",
                            color = InkMuted,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PersonTile(
    recipient: Recipient,
    index: Int,
    modifier: Modifier = Modifier,
) {
    val container = when (index % 3) {
        0 -> LavenderSurface
        1 -> BlueSurface
        else -> SurfaceWhite
    }
    val accent = if (index % 2 == 0) BrandIndigo else BrandBlue
    val cue = recipient.interests.firstOrNull()
        ?: recipient.personalityTraits?.asChips()?.firstOrNull()
        ?: "Gift profile saved"

    Surface(
        modifier = modifier
            .height(202.dp)
            .semantics {
                contentDescription = buildString {
                    append(recipient.displayName)
                    append(", ")
                    append(recipient.relationship)
                    append(", ")
                    append(cue)
                }
            },
        color = container,
        contentColor = Ink,
        shape = RoundedCornerShape(26.dp),
        border = if (container == SurfaceWhite) BorderStroke(1.dp, OutlineCool) else null,
    ) {
        Column(
            modifier = Modifier.padding(15.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                RecipientAvatar(
                    initials = recipient.initials,
                    photoUri = recipient.photoUri,
                    size = 66.dp,
                )
                Surface(
                    modifier = Modifier.size(34.dp),
                    color = SurfaceWhite.copy(alpha = 0.82f),
                    contentColor = accent,
                    shape = CircleShape,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.Favorite,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = recipient.displayName,
                    color = Ink,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = recipient.relationship,
                    color = InkMuted,
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Surface(
                    color = SurfaceWhite.copy(alpha = 0.78f),
                    contentColor = accent,
                    shape = CircleShape,
                ) {
                    Text(
                        text = cue,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyPeople(
    onAddPerson: () -> Unit,
    modifier: Modifier = Modifier,
) {
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
            text = "Who matters to you?",
            modifier = Modifier.semantics { heading() },
            color = Ink,
            style = MaterialTheme.typography.headlineMedium,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Add their next important moment.",
            color = InkMuted,
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
private fun LoadingState(modifier: Modifier = Modifier) {
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
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Surface(
            modifier = Modifier.size(82.dp),
            color = BlueSurface,
            contentColor = BrandBlue,
            shape = CircleShape,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Rounded.People,
                    contentDescription = null,
                    modifier = Modifier.size(38.dp),
                )
            }
        }
        Spacer(modifier = Modifier.height(18.dp))
        Text(
            text = "People couldn't load",
            modifier = Modifier.semantics { heading() },
            color = Ink,
            style = MaterialTheme.typography.headlineMedium,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message,
            color = InkMuted,
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
