package com.wishtrace.app.ui.screens.message

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wishtrace.app.R
import com.wishtrace.app.data.PreviewFixtures
import com.wishtrace.app.domain.HomeSnapshot
import com.wishtrace.app.domain.MessageOrigin
import com.wishtrace.app.domain.PersonalMessage
import com.wishtrace.app.ui.WishTraceTestTags
import com.wishtrace.app.ui.components.DimensionalAsset
import com.wishtrace.app.ui.components.ErrorBanner
import com.wishtrace.app.ui.components.PrimaryAction
import com.wishtrace.app.ui.components.RecipientAvatar
import com.wishtrace.app.ui.components.ScreenTopBar
import com.wishtrace.app.ui.components.SecondaryAction
import com.wishtrace.app.ui.theme.BlueSurface
import com.wishtrace.app.ui.theme.BrandBlue
import com.wishtrace.app.ui.theme.BrandIndigo
import com.wishtrace.app.ui.theme.BrandIndigoPressed
import com.wishtrace.app.ui.theme.Canvas
import com.wishtrace.app.ui.theme.Ink
import com.wishtrace.app.ui.theme.InkMuted
import com.wishtrace.app.ui.theme.LavenderSurface
import com.wishtrace.app.ui.theme.OutlineCool
import com.wishtrace.app.ui.theme.SurfaceWhite
import com.wishtrace.app.ui.theme.WishTraceTheme

@Composable
fun MessageRoute(
    viewModel: MessageViewModel,
    snapshot: HomeSnapshot,
    onBack: () -> Unit,
    onSaved: (PersonalMessage) -> Unit,
    onSkip: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    MessageScreen(
        snapshot = snapshot,
        state = state,
        onBack = onBack,
        onTextChange = viewModel::updateText,
        onSave = {
            viewModel.save(snapshot.recipient.id)?.let(onSaved)
        },
        onSkip = onSkip,
    )
}

@Composable
fun MessageScreen(
    snapshot: HomeSnapshot,
    state: MessageUiState,
    onBack: () -> Unit,
    onTextChange: (String) -> Unit,
    onSave: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.testTag(WishTraceTestTags.MessageScreen),
        containerColor = Canvas,
        topBar = {
            ScreenTopBar(
                title = "Personal note",
                onBack = onBack,
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp),
            )
        },
        bottomBar = {
            MessageActionBar(
                saveEnabled = state.text.isNotBlank(),
                onSave = onSave,
                onSkip = onSkip,
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            MessageRecipientPill(snapshot)
            MessageVisual()
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Make it sound like you.",
                    modifier = Modifier.semantics { heading() },
                    color = Ink,
                    style = MaterialTheme.typography.headlineLarge,
                )
                Text(
                    text = "A few honest words are enough.",
                    color = InkMuted,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }

            if (state.origin == MessageOrigin.GENERATED) {
                Surface(
                    color = LavenderSurface,
                    contentColor = BrandIndigoPressed,
                    shape = CircleShape,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Lightbulb,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text = "Generated draft · edit anything",
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                }
            }

            OutlinedTextField(
                value = state.text,
                onValueChange = onTextChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 190.dp),
                label = { Text("Your note") },
                placeholder = {
                    Text("Happy birthday…")
                },
                supportingText = {
                    Text("${state.text.length}/${MessageViewModel.MaxMessageLength}")
                },
                isError = state.error != null,
                shape = RoundedCornerShape(22.dp),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                ),
                minLines = 6,
                maxLines = 10,
            )

            state.error?.let {
                ErrorBanner(
                    text = it,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun MessageRecipientPill(snapshot: HomeSnapshot) {
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
                    text = snapshot.occasion.kind.displayName,
                    color = InkMuted,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Surface(
                color = BlueSurface,
                contentColor = BrandBlue,
                shape = CircleShape,
            ) {
                Text(
                    text = "${snapshot.daysUntil} days",
                    modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
    }
}

@Composable
private fun MessageVisual() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 248.dp),
        color = BlueSurface,
        shape = RoundedCornerShape(30.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 248.dp),
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                modifier = Modifier.size(178.dp),
                color = SurfaceWhite.copy(alpha = 0.8f),
                shape = CircleShape,
            ) {}
            DimensionalAsset(
                drawableRes = R.drawable.wishtrace_message_3d,
                description = null,
                modifier = Modifier.size(166.dp),
            )
            MessageOrbitIcon(
                icon = Icons.Rounded.CalendarMonth,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 28.dp, top = 28.dp)
                    .graphicsLayer { rotationZ = -6f },
            )
            MessageOrbitIcon(
                icon = Icons.Rounded.Favorite,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 28.dp, top = 46.dp)
                    .graphicsLayer { rotationZ = 7f },
            )
            MessageOrbitIcon(
                icon = Icons.Rounded.Shield,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 34.dp, bottom = 24.dp)
                    .graphicsLayer { rotationZ = -5f },
            )
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 28.dp, bottom = 28.dp)
                    .graphicsLayer { rotationZ = -4f },
                color = SurfaceWhite,
                contentColor = BrandIndigoPressed,
                shape = CircleShape,
                shadowElevation = 3.dp,
            ) {
                Text(
                    text = "FROM YOU",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
    }
}

@Composable
private fun MessageOrbitIcon(
    icon: ImageVector,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.size(52.dp),
        color = SurfaceWhite,
        contentColor = BrandIndigo,
        shape = RoundedCornerShape(17.dp),
        shadowElevation = 4.dp,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(23.dp),
            )
        }
    }
}

@Composable
private fun MessageActionBar(
    saveEnabled: Boolean,
    onSave: () -> Unit,
    onSkip: () -> Unit,
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
            PrimaryAction(
                text = "Save note",
                onClick = onSave,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(WishTraceTestTags.SaveMessage),
                enabled = saveEnabled,
            )
            SecondaryAction(
                text = "Skip for now",
                onClick = onSkip,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun MessagePreview() {
    WishTraceTheme {
        MessageScreen(
            snapshot = PreviewFixtures.homeSnapshot(),
            state = MessageUiState(),
            onBack = {},
            onTextChange = {},
            onSave = {},
            onSkip = {},
        )
    }
}
