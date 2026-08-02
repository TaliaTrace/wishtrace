package com.wishtrace.app.ui.screens.giftdna

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wishtrace.app.domain.RecurringFrequency
import com.wishtrace.app.ui.WishTraceTestTags
import com.wishtrace.app.ui.components.ErrorBanner
import com.wishtrace.app.ui.components.PrimaryAction
import com.wishtrace.app.ui.components.rememberWishTraceMotionEnabled
import com.wishtrace.app.ui.screens.giftdna.GiftDnaViewModel.Companion.datePickerUtcMillisToLocalDate
import com.wishtrace.app.ui.theme.BlueSurface
import com.wishtrace.app.ui.theme.BrandBlue
import com.wishtrace.app.ui.theme.BrandIndigo
import com.wishtrace.app.ui.theme.LavenderSurface
import com.wishtrace.app.ui.theme.Success
import com.wishtrace.app.ui.theme.SuccessSurface
import com.wishtrace.app.ui.theme.Warning
import com.wishtrace.app.ui.theme.WarningSurface
import com.wishtrace.app.ui.theme.ErrorRed
import com.wishtrace.app.ui.theme.ErrorSoft
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Full-bleed background per tile: 🔴 Red, 🔵 Blue, 🟢 Green, 🟡 Yellow. */
private data class TileIdentity(
    val tag: String,
    val background: Color,
    val chipContainer: Color,
    val chipContent: Color,
    val heading: Color,
    val accent: Color,
)

private fun tileIdentity(tile: GiftDnaTile): TileIdentity = when (tile) {
    GiftDnaTile.RED -> TileIdentity(
        tag = WishTraceTestTags.GiftDnaRedTile,
        background = ErrorSoft,
        chipContainer = Color.White,
        chipContent = ErrorRed,
        heading = ErrorRed,
        accent = ErrorRed,
    )

    GiftDnaTile.BLUE -> TileIdentity(
        tag = WishTraceTestTags.GiftDnaBlueTile,
        background = BlueSurface,
        chipContainer = Color.White,
        chipContent = BrandBlue,
        heading = BrandBlue,
        accent = BrandBlue,
    )

    GiftDnaTile.GREEN -> TileIdentity(
        tag = WishTraceTestTags.GiftDnaGreenTile,
        background = SuccessSurface,
        chipContainer = Color.White,
        chipContent = Success,
        heading = Success,
        accent = Success,
    )

    GiftDnaTile.YELLOW -> TileIdentity(
        tag = WishTraceTestTags.GiftDnaYellowTile,
        background = WarningSurface,
        chipContainer = Color.White,
        chipContent = Warning,
        heading = Warning,
        accent = Warning,
    )
}

@Composable
fun GiftDnaRoute(
    viewModel: GiftDnaViewModel,
    onBack: () -> Unit,
    onSaved: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.saveCompleted) {
        if (state.saveCompleted) onSaved()
    }

    GiftDnaScreen(
        state = state,
        onBack = {
            if (state.tile == GiftDnaTile.RED) onBack() else viewModel.back()
        },
        onNameChange = viewModel::updateName,
        onRelationshipSelect = viewModel::selectRelationship,
        onAgeBandSelect = viewModel::selectAgeBand,
        onDateChange = viewModel::updateDate,
        onEnergySelect = viewModel::selectEnergy,
        onEnvironmentSelect = viewModel::selectEnvironment,
        onStyleSelect = viewModel::selectStyle,
        onBudgetChange = viewModel::updateBudget,
        onFrequencySelect = viewModel::selectFrequency,
        onAdvance = viewModel::advance,
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun GiftDnaScreen(
    state: GiftDnaUiState,
    onBack: () -> Unit,
    onNameChange: (String) -> Unit,
    onRelationshipSelect: (RelationshipChoice) -> Unit,
    onAgeBandSelect: (AgeBandChoice) -> Unit,
    onDateChange: (LocalDate) -> Unit,
    onEnergySelect: (EnergyChoice) -> Unit,
    onEnvironmentSelect: (EnvironmentChoice) -> Unit,
    onStyleSelect: (StyleChoice) -> Unit,
    onBudgetChange: (Long) -> Unit,
    onFrequencySelect: (RecurringFrequency) -> Unit,
    onAdvance: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    val motionEnabled = rememberWishTraceMotionEnabled()
    var showDatePicker by rememberSaveable { mutableStateOf(false) }

    BackHandler(onBack = onBack)

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = state.occasionDate
                ?.atStartOfDay(ZoneOffset.UTC)
                ?.toInstant()
                ?.toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let {
                            onDateChange(datePickerUtcMillisToLocalDate(it))
                        }
                        showDatePicker = false
                    },
                    enabled = datePickerState.selectedDateMillis != null,
                ) {
                    Text("Choose")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }

    val identity = tileIdentity(state.tile)

    Scaffold(
        modifier = modifier.testTag(WishTraceTestTags.GiftDnaScreen),
        containerColor = identity.background,
        bottomBar = {
            GiftDnaActionBar(
                tile = state.tile,
                saving = state.saving,
                onAdvance = {
                    focusManager.clearFocus()
                    onAdvance()
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            GiftDnaProgress(
                tile = state.tile,
                container = identity.chipContainer,
                accent = identity.accent,
            )
            AnimatedContent(
                targetState = state.tile,
                modifier = Modifier
                    .fillMaxSize()
                    .testTag(identity.tag),
                transitionSpec = {
                    giftDnaTransition(
                        forward = targetState.ordinal > initialState.ordinal,
                        enabled = motionEnabled,
                    )
                },
                label = "gift dna tile",
            ) { tile ->
                when (tile) {
                    GiftDnaTile.RED -> RedTile(
                        state = state,
                        identity = identity,
                        onNameChange = onNameChange,
                        onRelationshipSelect = onRelationshipSelect,
                        onAgeBandSelect = onAgeBandSelect,
                    )

                    GiftDnaTile.BLUE -> BlueTile(
                        state = state,
                        identity = identity,
                        onPickDate = { showDatePicker = true },
                    )

                    GiftDnaTile.GREEN -> GreenTile(
                        state = state,
                        identity = identity,
                        onEnergySelect = onEnergySelect,
                        onEnvironmentSelect = onEnvironmentSelect,
                        onStyleSelect = onStyleSelect,
                    )

                    GiftDnaTile.YELLOW -> YellowTile(
                        state = state,
                        identity = identity,
                        onBudgetChange = onBudgetChange,
                        onFrequencySelect = onFrequencySelect,
                    )
                }
            }
        }
    }
}

@Composable
private fun GiftDnaProgress(
    tile: GiftDnaTile,
    container: Color,
    accent: Color,
) {
    Column(
        modifier = Modifier
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                color = container,
                contentColor = accent,
                shape = RoundedCornerShape(14.dp),
            ) {
                Text(
                    text = tile.title(),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
            Text(
                text = "${tile.ordinal + 1} of ${GiftDnaTile.entries.size}",
                color = accent.copy(alpha = 0.8f),
                style = MaterialTheme.typography.labelMedium,
            )
        }
        LinearProgressIndicator(
            progress = { (tile.ordinal + 1) / GiftDnaTile.entries.size.toFloat() },
            modifier = Modifier
                .fillMaxWidth()
                .height(5.dp),
            color = accent,
            trackColor = container.copy(alpha = 0.6f),
        )
    }
}

@Composable
private fun RedTile(
    state: GiftDnaUiState,
    identity: TileIdentity,
    onNameChange: (String) -> Unit,
    onRelationshipSelect: (RelationshipChoice) -> Unit,
    onAgeBandSelect: (AgeBandChoice) -> Unit,
) {
    val nameFocus = remember { FocusRequester() }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .imePadding(),
        contentPadding = PaddingValues(
            start = 20.dp,
            end = 20.dp,
            top = 12.dp,
            bottom = 28.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(22.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "🔴 Who's this for?",
                    modifier = Modifier.semantics { heading() },
                    color = identity.heading,
                    style = MaterialTheme.typography.headlineLarge,
                )
                Text(
                    text = "Start with the person you never want to miss.",
                    color = identity.heading.copy(alpha = 0.75f),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
        item {
            OutlinedTextField(
                value = state.displayName,
                onValueChange = onNameChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(nameFocus),
                label = { Text("Their name") },
                singleLine = true,
                isError = state.nameError != null,
                supportingText = state.nameError?.let { { Text(it) } },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next,
                ),
                shape = RoundedCornerShape(18.dp),
            )
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Who are they to you?",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = identity.heading,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    RelationshipChoice.entries.forEach { choice ->
                        val selected = state.relationship.equals(
                            choice.label,
                            ignoreCase = true,
                        )
                        FilterChip(
                            selected = selected,
                            onClick = { onRelationshipSelect(choice) },
                            label = { Text("${choice.emoji} ${choice.label}") },
                            leadingIcon = if (selected) {
                                {
                                    Icon(
                                        imageVector = Icons.Rounded.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                    )
                                }
                            } else {
                                null
                            },
                            colors = tileChipColors(identity),
                        )
                    }
                }
                state.relationshipError?.let {
                    FieldError(text = it, color = identity.accent)
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Their age band (optional)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = identity.heading,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    AgeBandChoice.entries.forEach { choice ->
                        val selected = state.ageBand == choice
                        FilterChip(
                            selected = selected,
                            onClick = { onAgeBandSelect(choice) },
                            label = { Text(choice.label) },
                            leadingIcon = if (selected) {
                                {
                                    Icon(
                                        imageVector = Icons.Rounded.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                    )
                                }
                            } else {
                                null
                            },
                            colors = tileChipColors(identity),
                        )
                    }
                }
            }
        }
        state.saveError?.let { error ->
            item { ErrorBanner(text = error) }
        }
    }

    LaunchedEffect(Unit) {
        if (state.displayName.isEmpty()) nameFocus.requestFocus()
    }
}

@Composable
private fun BlueTile(
    state: GiftDnaUiState,
    identity: TileIdentity,
    onPickDate: () -> Unit,
) {
    val dateFormatter = remember {
        DateTimeFormatter.ofPattern("EEE, MMM d, yyyy", Locale.US)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .imePadding(),
        contentPadding = PaddingValues(
            start = 20.dp,
            end = 20.dp,
            top = 12.dp,
            bottom = 28.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(22.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "🔵 What's the moment?",
                    modifier = Modifier.semantics { heading() },
                    color = identity.heading,
                    style = MaterialTheme.typography.headlineLarge,
                )
                Text(
                    text = "We'll build the reminder around one date.",
                    color = identity.heading.copy(alpha = 0.75f),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Occasion",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = identity.heading,
                )
                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(18.dp),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 15.dp, vertical = 13.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(text = "🎂", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = "Birthday",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = identity.heading,
                        )
                    }
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Date",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = identity.heading,
                )
                Surface(
                    onClick = onPickDate,
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics {
                            contentDescription = if (state.occasionDate == null) {
                                "Choose birthday date"
                            } else {
                                "Birthday ${state.occasionDate.format(dateFormatter)}"
                            }
                        },
                    color = Color.White,
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(
                        1.dp,
                        if (state.dateError == null) {
                            MaterialTheme.colorScheme.outline
                        } else {
                            MaterialTheme.colorScheme.error
                        },
                    ),
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CalendarMonth,
                            contentDescription = null,
                            tint = identity.accent,
                        )
                        Text(
                            text = state.occasionDate?.format(dateFormatter) ?: "Choose date",
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
                state.dateError?.let {
                    FieldError(text = it, color = identity.accent)
                }
            }
        }
        state.saveError?.let { error ->
            item { ErrorBanner(text = error) }
        }
    }
}

@Composable
private fun GreenTile(
    state: GiftDnaUiState,
    identity: TileIdentity,
    onEnergySelect: (EnergyChoice) -> Unit,
    onEnvironmentSelect: (EnvironmentChoice) -> Unit,
    onStyleSelect: (StyleChoice) -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .imePadding(),
        contentPadding = PaddingValues(
            start = 20.dp,
            end = 20.dp,
            top = 12.dp,
            bottom = 28.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(22.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "🟢 What are they like?",
                    modifier = Modifier.semantics { heading() },
                    color = identity.heading,
                    style = MaterialTheme.typography.headlineLarge,
                )
                Text(
                    text = "Three quick either/or taps. Skip anything you're not sure about.",
                    color = identity.heading.copy(alpha = 0.75f),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
        item {
            AxisQuestion(
                title = "How do they recharge?",
                identity = identity,
            ) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    EnergyChoice.entries.forEach { choice ->
                        val selected = state.energy == choice
                        FilterChip(
                            selected = selected,
                            onClick = { onEnergySelect(choice) },
                            label = { Text("${choice.emoji} ${choice.label}") },
                            leadingIcon = if (selected) {
                                {
                                    Icon(
                                        imageVector = Icons.Rounded.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                    )
                                }
                            } else {
                                null
                            },
                            colors = tileChipColors(identity),
                        )
                    }
                }
            }
        }
        item {
            AxisQuestion(
                title = "Where do they spend their time?",
                identity = identity,
            ) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    EnvironmentChoice.entries.forEach { choice ->
                        val selected = state.environment == choice
                        FilterChip(
                            selected = selected,
                            onClick = { onEnvironmentSelect(choice) },
                            label = { Text("${choice.emoji} ${choice.label}") },
                            leadingIcon = if (selected) {
                                {
                                    Icon(
                                        imageVector = Icons.Rounded.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                    )
                                }
                            } else {
                                null
                            },
                            colors = tileChipColors(identity),
                        )
                    }
                }
            }
        }
        item {
            AxisQuestion(
                title = "What do they gravitate toward?",
                identity = identity,
            ) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    StyleChoice.entries.forEach { choice ->
                        val selected = state.style == choice
                        FilterChip(
                            selected = selected,
                            onClick = { onStyleSelect(choice) },
                            label = { Text("${choice.emoji} ${choice.label}") },
                            leadingIcon = if (selected) {
                                {
                                    Icon(
                                        imageVector = Icons.Rounded.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                    )
                                }
                            } else {
                                null
                            },
                            colors = tileChipColors(identity),
                        )
                    }
                }
            }
        }
        state.saveError?.let { error ->
            item { ErrorBanner(text = error) }
        }
    }
}

@Composable
private fun AxisQuestion(
    title: String,
    identity: TileIdentity,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = identity.heading,
        )
        content()
    }
}

@Composable
private fun YellowTile(
    state: GiftDnaUiState,
    identity: TileIdentity,
    onBudgetChange: (Long) -> Unit,
    onFrequencySelect: (RecurringFrequency) -> Unit,
) {
    val sliderRange = GiftDnaUiState.MIN_BUDGET_MINOR..GiftDnaUiState.MAX_BUDGET_MINOR

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .imePadding(),
        contentPadding = PaddingValues(
            start = 20.dp,
            end = 20.dp,
            top = 12.dp,
            bottom = 28.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(22.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "🟡 How much, how often?",
                    modifier = Modifier.semantics { heading() },
                    color = identity.heading,
                    style = MaterialTheme.typography.headlineLarge,
                )
                Text(
                    text = "Set the cap. Choose once, or every year automatically.",
                    color = identity.heading.copy(alpha = 0.75f),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Gift budget",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = identity.heading,
                )
                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(20.dp),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = state.budgetMinorUnits.asUsd(),
                            color = identity.heading,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Slider(
                            value = state.budgetMinorUnits.toFloat(),
                            onValueChange = { onBudgetChange(it.toLong()) },
                            valueRange = sliderRange.first.toFloat()..sliderRange.last.toFloat(),
                            colors = SliderDefaults.colors(
                                thumbColor = identity.accent,
                                activeTrackColor = identity.accent,
                            ),
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = "$5",
                                color = identity.heading.copy(alpha = 0.7f),
                                style = MaterialTheme.typography.labelMedium,
                            )
                            Text(
                                text = "$50",
                                color = identity.heading.copy(alpha = 0.7f),
                                style = MaterialTheme.typography.labelMedium,
                            )
                        }
                    }
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "How often?",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = identity.heading,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    RecurringFrequency.entries.forEach { frequency ->
                        val selected = state.frequency == frequency
                        FilterChip(
                            selected = selected,
                            onClick = { onFrequencySelect(frequency) },
                            label = { Text(frequency.displayName) },
                            leadingIcon = if (selected) {
                                {
                                    Icon(
                                        imageVector = Icons.Rounded.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                    )
                                }
                            } else {
                                null
                            },
                            colors = tileChipColors(identity),
                        )
                    }
                }
            }
        }
        item {
            Surface(
                color = Color.White,
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
                        imageVector = Icons.Rounded.Lightbulb,
                        contentDescription = null,
                        tint = identity.accent,
                    )
                    Text(
                        text = if (state.frequency == RecurringFrequency.YEARLY) {
                            "Yearly stays within this cap every time. Approve once, then it runs."
                        } else {
                            "One-time stays within this cap. Approve once, and we'll handle the rest."
                        },
                        color = identity.heading.copy(alpha = 0.8f),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
        state.saveError?.let { error ->
            item { ErrorBanner(text = error) }
        }
    }
}

@Composable
private fun GiftDnaActionBar(
    tile: GiftDnaTile,
    saving: Boolean,
    onAdvance: () -> Unit,
) {
    Surface(
        color = Color.White.copy(alpha = 0.92f),
        shadowElevation = 8.dp,
    ) {
        PrimaryAction(
            text = when {
                saving -> "Saving"
                tile == GiftDnaTile.YELLOW -> "Create and arm"
                tile == GiftDnaTile.GREEN -> "Skip or continue"
                tile == GiftDnaTile.BLUE -> "Continue"
                else -> "Next"
            },
            onClick = onAdvance,
            enabled = !saving,
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 14.dp)
                .testTag(WishTraceTestTags.GiftDnaPrimaryAction),
            trailingContent = if (saving) {
                {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp,
                    )
                }
            } else {
                null
            },
        )
    }
}

@Composable
private fun FieldError(text: String, color: Color) {
    Text(
        text = text,
        modifier = Modifier.padding(start = 16.dp),
        color = color,
        style = MaterialTheme.typography.bodyMedium,
    )
}

@Composable
private fun tileChipColors(identity: TileIdentity) = FilterChipDefaults.filterChipColors(
    selectedContainerColor = identity.chipContainer,
    selectedLabelColor = identity.chipContent,
    selectedLeadingIconColor = identity.chipContent,
)

private fun giftDnaTransition(
    forward: Boolean,
    enabled: Boolean,
): ContentTransform {
    if (!enabled) return EnterTransition.None togetherWith ExitTransition.None
    val direction = if (forward) 1 else -1
    return (
        fadeIn(tween(180)) +
            slideInHorizontally(tween(240)) { fullWidth -> fullWidth * direction / 7 }
        ) togetherWith (
        fadeOut(tween(120)) +
            slideOutHorizontally(tween(180)) { fullWidth -> -fullWidth * direction / 10 }
        )
}

private fun GiftDnaTile.title(): String = when (this) {
    GiftDnaTile.RED -> "Recipient"
    GiftDnaTile.BLUE -> "Occasion"
    GiftDnaTile.GREEN -> "Personality"
    GiftDnaTile.YELLOW -> "Budget & autopilot"
}

private fun Long.asUsd(): String {
    val dollars = this / 100L
    val cents = (this % 100L).toString().padStart(2, '0')
    return "$$dollars.$cents"
}
