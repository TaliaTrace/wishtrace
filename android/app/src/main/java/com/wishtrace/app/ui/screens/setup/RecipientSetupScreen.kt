package com.wishtrace.app.ui.screens.setup

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Cake
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wishtrace.app.ui.components.ErrorBanner
import com.wishtrace.app.ui.components.PrimaryAction
import com.wishtrace.app.ui.components.ScreenTopBar
import com.wishtrace.app.ui.components.rememberWishTraceMotionEnabled
import com.wishtrace.app.ui.theme.BlueSurface
import com.wishtrace.app.ui.theme.BrandBlue
import com.wishtrace.app.ui.theme.BrandIndigo
import com.wishtrace.app.ui.theme.LavenderSurface
import com.wishtrace.app.ui.WishTraceTestTags
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

private val interestOptions = listOf(
    "Cozy gaming",
    "Live music",
    "Books",
    "Food",
    "Fitness",
    "Travel",
)

private val relationshipOptions = listOf(
    "Friend",
    "Partner",
    "Family",
    "Sibling",
)

@Composable
fun RecipientSetupRoute(
    viewModel: RecipientSetupViewModel,
    title: String,
    occasionOnly: Boolean,
    onBack: () -> Unit,
    onSaved: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.saveCompleted) {
        if (state.saveCompleted) onSaved()
    }

    RecipientSetupScreen(
        state = state,
        title = title,
        occasionOnly = occasionOnly,
        onBack = {
            if (state.step == RecipientSetupStep.OCCASION && !occasionOnly) {
                viewModel.backToPerson()
            } else {
                onBack()
            }
        },
        onNameChange = viewModel::updateName,
        onRelationshipChange = viewModel::updateRelationship,
        onDateChange = viewModel::updateDate,
        onInterestToggle = viewModel::toggleInterest,
        onDislikesChange = viewModel::updateDislikes,
        onBudgetChange = viewModel::updateBudget,
        onHintChange = viewModel::updateHint,
        onContinue = viewModel::continueToOccasion,
        onSave = viewModel::save,
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RecipientSetupScreen(
    state: RecipientSetupUiState,
    title: String,
    occasionOnly: Boolean,
    onBack: () -> Unit,
    onNameChange: (String) -> Unit,
    onRelationshipChange: (String) -> Unit,
    onDateChange: (LocalDate) -> Unit,
    onInterestToggle: (String) -> Unit,
    onDislikesChange: (String) -> Unit,
    onBudgetChange: (String) -> Unit,
    onHintChange: (String) -> Unit,
    onContinue: () -> Unit,
    onSave: () -> Unit,
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
                            onDateChange(
                                RecipientSetupValidator.datePickerUtcMillisToLocalDate(it),
                            )
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

    Scaffold(
        modifier = modifier.testTag(WishTraceTestTags.SetupScreen),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column(
                modifier = Modifier.statusBarsPadding(),
            ) {
                ScreenTopBar(
                    title = title,
                    onBack = onBack,
                    modifier = Modifier.padding(horizontal = 12.dp),
                )
                if (!occasionOnly) {
                    SetupProgress(step = state.step)
                }
            }
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.background,
                shadowElevation = 0.dp,
            ) {
                PrimaryAction(
                    text = when {
                        state.saving -> "Saving"
                        state.step == RecipientSetupStep.PERSON -> "Next"
                        else -> "Save"
                    },
                    onClick = {
                        focusManager.clearFocus()
                        if (state.step == RecipientSetupStep.PERSON) {
                            onContinue()
                        } else {
                            onSave()
                        }
                    },
                    enabled = !state.saving,
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 14.dp)
                        .testTag(WishTraceTestTags.SetupPrimaryAction),
                    trailingContent = if (state.saving) {
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
        },
    ) { innerPadding ->
        AnimatedContent(
            targetState = state.step,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            transitionSpec = {
                setupTransition(
                    forward = targetState == RecipientSetupStep.OCCASION,
                    enabled = motionEnabled,
                )
            },
            label = "recipient setup step",
        ) { step ->
            when (step) {
                RecipientSetupStep.PERSON -> PersonStep(
                    state = state,
                    onNameChange = onNameChange,
                    onRelationshipChange = onRelationshipChange,
                )

                RecipientSetupStep.OCCASION -> OccasionStep(
                    state = state,
                    occasionOnly = occasionOnly,
                    onPickDate = { showDatePicker = true },
                    onInterestToggle = onInterestToggle,
                    onDislikesChange = onDislikesChange,
                    onBudgetChange = onBudgetChange,
                    onHintChange = onHintChange,
                )
            }
        }
    }
}

@Composable
private fun SetupProgress(step: RecipientSetupStep) {
    Column(
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = if (step == RecipientSetupStep.PERSON) "Person" else "Occasion",
                style = MaterialTheme.typography.labelMedium,
                color = BrandIndigo,
            )
            Text(
                text = if (step == RecipientSetupStep.PERSON) "1 of 2" else "2 of 2",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        LinearProgressIndicator(
            progress = { if (step == RecipientSetupStep.PERSON) 0.5f else 1f },
            modifier = Modifier
                .fillMaxWidth()
                .height(5.dp),
            color = BrandIndigo,
            trackColor = LavenderSurface,
        )
    }
}

@Composable
private fun PersonStep(
    state: RecipientSetupUiState,
    onNameChange: (String) -> Unit,
    onRelationshipChange: (String) -> Unit,
) {
    val nameFocus = remember { FocusRequester() }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag(WishTraceTestTags.SetupPersonStep)
            .imePadding(),
        contentPadding = PaddingValues(
            start = 20.dp,
            end = 20.dp,
            top = 24.dp,
            bottom = 28.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item {
            Text(
                text = "Who are they?",
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.headlineLarge,
            )
        }
        item {
            OutlinedTextField(
                value = state.displayName,
                onValueChange = onNameChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(nameFocus),
                label = { Text("Name") },
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
                    text = "Relationship",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    relationshipOptions.forEach { relationship ->
                        FilterChip(
                            selected = state.relationship.equals(
                                relationship,
                                ignoreCase = true,
                            ),
                            onClick = { onRelationshipChange(relationship) },
                            label = { Text(relationship) },
                            leadingIcon = if (
                                state.relationship.equals(relationship, ignoreCase = true)
                            ) {
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
                            colors = setupChipColors(),
                        )
                    }
                }
                OutlinedTextField(
                    value = state.relationship,
                    onValueChange = onRelationshipChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Or type it") },
                    singleLine = true,
                    isError = state.relationshipError != null,
                    supportingText = state.relationshipError?.let { { Text(it) } },
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Done,
                    ),
                    shape = RoundedCornerShape(18.dp),
                )
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
private fun OccasionStep(
    state: RecipientSetupUiState,
    occasionOnly: Boolean,
    onPickDate: () -> Unit,
    onInterestToggle: (String) -> Unit,
    onDislikesChange: (String) -> Unit,
    onBudgetChange: (String) -> Unit,
    onHintChange: (String) -> Unit,
) {
    val dateFormatter = remember {
        DateTimeFormatter.ofPattern("EEE, MMM d, yyyy", Locale.US)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag(WishTraceTestTags.SetupOccasionStep)
            .imePadding(),
        contentPadding = PaddingValues(
            start = 20.dp,
            end = 20.dp,
            top = 24.dp,
            bottom = 28.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(22.dp),
    ) {
        item {
            Text(
                text = if (occasionOnly) "Birthday details" else "What should we remember?",
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.headlineLarge,
            )
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Occasion",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                InfoPill(
                    icon = Icons.Rounded.Cake,
                    title = "Birthday",
                    tint = BrandIndigo,
                    container = LavenderSurface,
                )
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Date",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
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
                    color = BlueSurface,
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
                            tint = BrandBlue,
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
                    FieldError(text = it)
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "They enjoy",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    interestOptions.forEach { interest ->
                        val selected = interest in state.selectedInterests
                        FilterChip(
                            selected = selected,
                            onClick = { onInterestToggle(interest) },
                            label = { Text(interest) },
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
                            colors = setupChipColors(),
                        )
                    }
                }
                state.interestsError?.let { FieldError(it) }
            }
        }
        item {
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        }
        item {
            OutlinedTextField(
                value = state.dislikesText,
                onValueChange = onDislikesChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Avoid") },
                placeholder = { Text("Clutter, strong scents") },
                supportingText = { Text("Separate with commas") },
                maxLines = 2,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Next,
                ),
                shape = RoundedCornerShape(18.dp),
            )
        }
        item {
            OutlinedTextField(
                value = state.budgetText,
                onValueChange = { value ->
                    if (value.matches(Regex("^\\d{0,6}(\\.\\d{0,2})?$"))) {
                        onBudgetChange(value)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Gift budget") },
                prefix = { Text("$") },
                suffix = { Text("USD") },
                singleLine = true,
                isError = state.budgetError != null,
                supportingText = state.budgetError?.let { { Text(it) } },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Next,
                ),
                shape = RoundedCornerShape(18.dp),
            )
        }
        item {
            OutlinedTextField(
                value = state.hintText,
                onValueChange = onHintChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("A clue (optional)") },
                placeholder = { Text("Something they mentioned recently") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Lightbulb,
                        contentDescription = null,
                        tint = BrandBlue,
                    )
                },
                minLines = 2,
                maxLines = 3,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Done,
                ),
                shape = RoundedCornerShape(18.dp),
            )
        }
        state.saveError?.let { error ->
            item { ErrorBanner(text = error) }
        }
    }
}

@Composable
private fun InfoPill(
    icon: ImageVector,
    title: String,
    tint: androidx.compose.ui.graphics.Color,
    container: androidx.compose.ui.graphics.Color,
) {
    Surface(
        color = container,
        contentColor = tint,
        shape = RoundedCornerShape(18.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 15.dp, vertical = 13.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun FieldError(text: String) {
    Text(
        text = text,
        modifier = Modifier.padding(start = 16.dp),
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodyMedium,
    )
}

@Composable
private fun setupChipColors() = FilterChipDefaults.filterChipColors(
    selectedContainerColor = LavenderSurface,
    selectedLabelColor = BrandIndigo,
    selectedLeadingIconColor = BrandIndigo,
)

private fun setupTransition(
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
