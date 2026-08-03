package com.wishtrace.app.ui.screens.giftdna

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.Cake
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChildCare
import androidx.compose.material.icons.rounded.Devices
import androidx.compose.material.icons.rounded.Elderly
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LooksOne
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Paid
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.Work
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wishtrace.app.domain.RecurringFrequency
import com.wishtrace.app.data.importChosenContact
import com.wishtrace.app.ui.WishTraceTestTags
import com.wishtrace.app.ui.components.ErrorBanner
import com.wishtrace.app.ui.components.PrimaryAction
import com.wishtrace.app.ui.components.RecipientAvatar
import com.wishtrace.app.ui.components.rememberWishTraceMotionEnabled
import com.wishtrace.app.ui.screens.giftdna.GiftDnaViewModel.Companion.datePickerUtcMillisToLocalDate
import com.wishtrace.app.ui.theme.BlueSurface
import com.wishtrace.app.ui.theme.BrandBlue
import com.wishtrace.app.ui.theme.BrandIndigo
import com.wishtrace.app.ui.theme.Canvas
import com.wishtrace.app.ui.theme.CoolCyan
import com.wishtrace.app.ui.theme.CoolCyanSurface
import com.wishtrace.app.ui.theme.Ink
import com.wishtrace.app.ui.theme.LavenderSurface
import com.wishtrace.app.ui.theme.OutlineCool
import com.wishtrace.app.ui.theme.Periwinkle
import com.wishtrace.app.ui.theme.PeriwinkleSurface
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.launch

private data class TileIdentity(
    val tag: String,
    val accent: Color,
    val soft: Color,
    val selectedContainer: Color,
    val selectedContent: Color,
)

private fun tileIdentity(tile: GiftDnaTile): TileIdentity = when (tile) {
    GiftDnaTile.RED -> TileIdentity(
        tag = WishTraceTestTags.GiftDnaRedTile,
        accent = BrandIndigo,
        soft = LavenderSurface,
        selectedContainer = BrandIndigo,
        selectedContent = Color.White,
    )

    GiftDnaTile.BLUE -> TileIdentity(
        tag = WishTraceTestTags.GiftDnaBlueTile,
        accent = BrandBlue,
        soft = BlueSurface,
        selectedContainer = BrandBlue,
        selectedContent = Color.White,
    )

    GiftDnaTile.GREEN -> TileIdentity(
        tag = WishTraceTestTags.GiftDnaGreenTile,
        accent = CoolCyan,
        soft = CoolCyanSurface,
        selectedContainer = CoolCyan,
        selectedContent = Color.White,
    )

    GiftDnaTile.YELLOW -> TileIdentity(
        tag = WishTraceTestTags.GiftDnaYellowTile,
        accent = Periwinkle,
        soft = PeriwinkleSurface,
        selectedContainer = Periwinkle,
        selectedContent = Color.White,
    )
}

@Composable
fun GiftDnaRoute(
    viewModel: GiftDnaViewModel,
    onBack: () -> Unit,
    onSaved: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val contactPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickContact(),
    ) { contactUri ->
        if (contactUri != null) {
            scope.launch {
                importChosenContact(context, contactUri)?.let { contact ->
                    viewModel.importContact(contact.displayName, contact.localPhotoUri)
                }
            }
        }
    }

    LaunchedEffect(state.saveCompleted) {
        if (state.saveCompleted) onSaved()
    }

    GiftDnaScreen(
        state = state,
        onBack = {
            if (state.tile == GiftDnaTile.RED) onBack() else viewModel.back()
        },
        onNameChange = viewModel::updateName,
        onPickContact = { contactPicker.launch(null) },
        onRelationshipSelect = viewModel::selectRelationship,
        onAgeBandSelect = viewModel::selectAgeBand,
        onDateChange = viewModel::updateDate,
        onInterestToggle = viewModel::toggleInterest,
        onEnergySelect = viewModel::selectEnergy,
        onBudgetChange = viewModel::updateBudget,
        onFrequencySelect = viewModel::selectFrequency,
        onAdvance = viewModel::advance,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GiftDnaScreen(
    state: GiftDnaUiState,
    onBack: () -> Unit,
    onNameChange: (String) -> Unit,
    onPickContact: () -> Unit,
    onRelationshipSelect: (RelationshipChoice) -> Unit,
    onAgeBandSelect: (AgeBandChoice) -> Unit,
    onDateChange: (LocalDate) -> Unit,
    onInterestToggle: (InterestChoice) -> Unit,
    onEnergySelect: (EnergyChoice) -> Unit,
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

    Scaffold(
        modifier = modifier.testTag(WishTraceTestTags.GiftDnaScreen),
        containerColor = Canvas,
        bottomBar = {
            GiftDnaActionBar(
                state = state,
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
                onBack = onBack,
                motionEnabled = motionEnabled,
            )
            AnimatedContent(
                targetState = state.tile,
                modifier = Modifier.fillMaxSize(),
                transitionSpec = {
                    giftDnaTransition(
                        forward = targetState.ordinal > initialState.ordinal,
                        enabled = motionEnabled,
                    )
                },
                label = "Gift DNA chapter",
            ) { tile ->
                val chapterIdentity = tileIdentity(tile)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag(chapterIdentity.tag),
                ) {
                    when (tile) {
                        GiftDnaTile.RED -> RedTile(
                            state = state,
                            identity = chapterIdentity,
                            motionEnabled = motionEnabled,
                            onNameChange = onNameChange,
                            onPickContact = onPickContact,
                            onRelationshipSelect = onRelationshipSelect,
                            onAgeBandSelect = onAgeBandSelect,
                        )

                        GiftDnaTile.BLUE -> BlueTile(
                            state = state,
                            identity = chapterIdentity,
                            onPickDate = { showDatePicker = true },
                        )

                        GiftDnaTile.GREEN -> GreenTile(
                            state = state,
                            identity = chapterIdentity,
                            motionEnabled = motionEnabled,
                            onInterestToggle = onInterestToggle,
                            onEnergySelect = onEnergySelect,
                        )

                        GiftDnaTile.YELLOW -> YellowTile(
                            state = state,
                            identity = chapterIdentity,
                            motionEnabled = motionEnabled,
                            onBudgetChange = onBudgetChange,
                            onFrequencySelect = onFrequencySelect,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GiftDnaProgress(
    tile: GiftDnaTile,
    onBack: () -> Unit,
    motionEnabled: Boolean,
) {
    Column(
        modifier = Modifier
            .statusBarsPadding()
            .padding(start = 8.dp, end = 20.dp, top = 8.dp, bottom = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Back",
                )
            }
            Text(
                text = "Gift DNA",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "${tile.ordinal + 1} / ${GiftDnaTile.entries.size}",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelMedium,
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GiftDnaTile.entries.forEach { item ->
                val active = item == tile
                val reached = item.ordinal <= tile.ordinal
                val width by animateDpAsState(
                    targetValue = if (active) 40.dp else 14.dp,
                    animationSpec = tween(if (motionEnabled) 220 else 0),
                    label = "Gift DNA progress width",
                )
                Surface(
                    modifier = Modifier
                        .height(8.dp)
                        .then(Modifier.size(width = width, height = 8.dp)),
                    color = progressColor(item).copy(alpha = if (reached) 1f else 0.22f),
                    shape = CircleShape,
                ) {}
            }
        }
    }
}

private fun progressColor(tile: GiftDnaTile): Color = when (tile) {
    GiftDnaTile.RED -> BrandIndigo
    GiftDnaTile.BLUE -> BrandBlue
    GiftDnaTile.GREEN -> CoolCyan
    GiftDnaTile.YELLOW -> Periwinkle
}

@Composable
private fun RedTile(
    state: GiftDnaUiState,
    identity: TileIdentity,
    motionEnabled: Boolean,
    onNameChange: (String) -> Unit,
    onPickContact: () -> Unit,
    onRelationshipSelect: (RelationshipChoice) -> Unit,
    onAgeBandSelect: (AgeBandChoice) -> Unit,
) {
    GiftDnaColumn {
        item {
            ChapterHeader(
                title = "Who matters to you?",
                subtitle = "Start with the person. We'll learn the rest in taps.",
                identity = identity,
            )
        }
        item {
            ContactPickerTile(
                state = state,
                identity = identity,
                onClick = onPickContact,
            )
        }
        item {
            OutlinedTextField(
                value = state.displayName,
                onValueChange = onNameChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Their name") },
                singleLine = true,
                isError = state.nameError != null,
                supportingText = state.nameError?.let { { Text(it) } },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next,
                ),
                shape = RoundedCornerShape(22.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = identity.accent,
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    errorContainerColor = Color.White,
                ),
            )
        }
        item {
            SectionLabel("Your connection")
            Spacer(modifier = Modifier.height(10.dp))
            BentoRows(RelationshipChoice.entries) { choice, itemModifier ->
                BentoChoice(
                    title = choice.label,
                    icon = choice.icon,
                    selected = state.relationship.equals(choice.label, ignoreCase = true),
                    identity = identity,
                    motionEnabled = motionEnabled,
                    onClick = { onRelationshipSelect(choice) },
                    modifier = itemModifier,
                )
            }
            state.relationshipError?.let {
                Spacer(modifier = Modifier.height(8.dp))
                FieldError(it, identity.accent)
            }
        }
        item {
            SectionLabel("Rough age", optional = true)
            Spacer(modifier = Modifier.height(10.dp))
            BentoRows(AgeBandChoice.entries) { choice, itemModifier ->
                BentoChoice(
                    title = choice.shortLabel,
                    icon = choice.icon,
                    selected = state.ageBand == choice,
                    identity = identity,
                    motionEnabled = motionEnabled,
                    minHeight = 76.dp,
                    onClick = { onAgeBandSelect(choice) },
                    modifier = itemModifier,
                )
            }
        }
        state.saveError?.let { error -> item { ErrorBanner(text = error) } }
    }
}

@Composable
private fun ContactPickerTile(
    state: GiftDnaUiState,
    identity: TileIdentity,
    onClick: () -> Unit,
) {
    val hasContact = state.displayName.isNotBlank() && state.photoUri != null
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        color = identity.soft,
        contentColor = Ink,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, identity.accent.copy(alpha = 0.16f)),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(13.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RecipientAvatar(
                initials = state.displayName
                    .trim()
                    .split(Regex("\\s+"))
                    .filter(String::isNotBlank)
                    .take(2)
                    .joinToString("") { it.take(1).uppercase(Locale.US) }
                    .ifBlank { "+" },
                photoUri = state.photoUri,
                size = 54.dp,
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = if (hasContact) state.displayName else "Choose from contacts",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = if (hasContact) "Photo imported on this phone" else "You choose exactly one person",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Text(
                text = if (hasContact) "Change" else "Choose",
                color = identity.accent,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun BlueTile(
    state: GiftDnaUiState,
    identity: TileIdentity,
    onPickDate: () -> Unit,
) {
    val fullDate = remember { DateTimeFormatter.ofPattern("EEE, MMM d, yyyy", Locale.US) }
    val month = remember { DateTimeFormatter.ofPattern("MMM", Locale.US) }

    GiftDnaColumn {
        item {
            ChapterHeader(
                title = "When should it feel special?",
                subtitle = "One date turns good intentions into a plan.",
                identity = identity,
            )
        }
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(184.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Surface(
                    modifier = Modifier.weight(0.82f),
                    color = identity.soft,
                    shape = RoundedCornerShape(26.dp),
                    border = BorderStroke(1.dp, identity.accent.copy(alpha = 0.14f)),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Surface(
                            color = Color.White,
                            contentColor = identity.accent,
                            shape = CircleShape,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Cake,
                                contentDescription = null,
                                modifier = Modifier.padding(10.dp),
                            )
                        }
                        Text(
                            text = "Birthday",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
                Surface(
                    onClick = onPickDate,
                    modifier = Modifier
                        .weight(1.18f)
                        .semantics {
                            contentDescription = state.occasionDate?.let {
                                "Birthday ${it.format(fullDate)}"
                            } ?: "Choose birthday date"
                        },
                    color = if (state.occasionDate == null) Color.White else identity.selectedContainer,
                    contentColor = if (state.occasionDate == null) Ink else identity.selectedContent,
                    shape = RoundedCornerShape(26.dp),
                    border = BorderStroke(
                        1.dp,
                        when {
                            state.dateError != null -> MaterialTheme.colorScheme.error
                            state.occasionDate != null -> identity.selectedContainer
                            else -> OutlineCool
                        },
                    ),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CalendarMonth,
                            contentDescription = null,
                            tint = if (state.occasionDate == null) identity.accent else identity.selectedContent,
                        )
                        if (state.occasionDate == null) {
                            Text(
                                text = "Pick\na date",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                            )
                        } else {
                            Column {
                                Text(
                                    text = state.occasionDate.format(month).uppercase(Locale.US),
                                    style = MaterialTheme.typography.labelMedium,
                                )
                                Text(
                                    text = state.occasionDate.dayOfMonth.toString(),
                                    style = MaterialTheme.typography.displayLarge,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }
                }
            }
            state.dateError?.let {
                Spacer(modifier = Modifier.height(8.dp))
                FieldError(it, MaterialTheme.colorScheme.error)
            }
        }
        state.occasionDate?.let { date ->
            item {
                Surface(
                    onClick = onPickDate,
                    modifier = Modifier.fillMaxWidth(),
                    color = Color.White,
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, OutlineCool),
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = null,
                            tint = identity.accent,
                        )
                        Text(
                            text = date.format(fullDate),
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.labelLarge,
                        )
                        Text(
                            text = "Change",
                            color = identity.accent,
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                }
            }
        }
        state.saveError?.let { error -> item { ErrorBanner(text = error) } }
    }
}

@Composable
private fun GreenTile(
    state: GiftDnaUiState,
    identity: TileIdentity,
    motionEnabled: Boolean,
    onInterestToggle: (InterestChoice) -> Unit,
    onEnergySelect: (EnergyChoice) -> Unit,
) {
    GiftDnaColumn {
        item {
            ChapterHeader(
                title = state.displayName.takeIf(String::isNotBlank)?.let {
                    "What lights $it up?"
                } ?: "What lights them up?",
                subtitle = "Pick up to three. These steer every gift decision.",
                identity = identity,
            )
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SectionLabel("Interests")
                Surface(
                    color = identity.soft,
                    contentColor = identity.accent,
                    shape = CircleShape,
                ) {
                    Text(
                        text = "${state.interests.size} / ${GiftDnaViewModel.MAX_INTERESTS}",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            BentoRows(InterestChoice.entries) { choice, itemModifier ->
                BentoChoice(
                    title = choice.label,
                    icon = choice.icon,
                    selected = choice in state.interests,
                    identity = identity,
                    motionEnabled = motionEnabled,
                    minHeight = 92.dp,
                    onClick = { onInterestToggle(choice) },
                    modifier = itemModifier,
                )
            }
        }
        item {
            SectionLabel("Their pace", optional = true)
            Spacer(modifier = Modifier.height(10.dp))
            BentoRows(EnergyChoice.entries) { choice, itemModifier ->
                BentoChoice(
                    title = choice.label,
                    icon = choice.icon,
                    selected = state.energy == choice,
                    identity = identity,
                    motionEnabled = motionEnabled,
                    minHeight = 88.dp,
                    onClick = { onEnergySelect(choice) },
                    modifier = itemModifier,
                )
            }
        }
        state.saveError?.let { error -> item { ErrorBanner(text = error) } }
    }
}

@Composable
private fun YellowTile(
    state: GiftDnaUiState,
    identity: TileIdentity,
    motionEnabled: Boolean,
    onBudgetChange: (Long) -> Unit,
    onFrequencySelect: (RecurringFrequency) -> Unit,
) {
    val budgetChoices = remember { listOf(500L, 1_000L, 2_500L, 5_000L) }

    GiftDnaColumn {
        item {
            ChapterHeader(
                title = "Set the guardrails",
                subtitle = "You choose the cap and rhythm. WishTrace stays inside them.",
                identity = identity,
            )
        }
        item {
            SectionLabel("Budget")
            Spacer(modifier = Modifier.height(10.dp))
            BentoRows(budgetChoices) { amount, itemModifier ->
                BentoChoice(
                    title = amount.asWholeUsd(),
                    icon = Icons.Rounded.Paid,
                    selected = state.budgetMinorUnits == amount,
                    identity = identity,
                    motionEnabled = motionEnabled,
                    minHeight = 88.dp,
                    onClick = { onBudgetChange(amount) },
                    modifier = itemModifier,
                )
            }
        }
        item {
            SectionLabel("Rhythm")
            Spacer(modifier = Modifier.height(10.dp))
            BentoRows(RecurringFrequency.entries) { frequency, itemModifier ->
                BentoChoice(
                    title = if (frequency == RecurringFrequency.YEARLY) "Every year" else "Just once",
                    subtitle = if (frequency == RecurringFrequency.YEARLY) "Same date, same cap" else "This birthday only",
                    icon = if (frequency == RecurringFrequency.YEARLY) Icons.Rounded.Repeat else Icons.Rounded.LooksOne,
                    selected = state.frequency == frequency,
                    identity = identity,
                    motionEnabled = motionEnabled,
                    minHeight = 112.dp,
                    onClick = { onFrequencySelect(frequency) },
                    modifier = itemModifier,
                )
            }
        }
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = identity.soft,
                contentColor = identity.accent,
                shape = RoundedCornerShape(22.dp),
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Shield,
                        contentDescription = null,
                    )
                    Text(
                        text = "Next: review a real gift before any payment attempt.",
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }
        state.saveError?.let { error -> item { ErrorBanner(text = error) } }
    }
}

@Composable
private fun GiftDnaColumn(
    content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .imePadding(),
        contentPadding = PaddingValues(
            start = 20.dp,
            end = 20.dp,
            top = 8.dp,
            bottom = 28.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        content = content,
    )
}

@Composable
private fun ChapterHeader(
    title: String,
    subtitle: String,
    identity: TileIdentity,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Surface(
            color = identity.soft,
            contentColor = identity.accent,
            shape = CircleShape,
        ) {
            Text(
                text = identity.tag.chapterLabel,
                modifier = Modifier.padding(horizontal = 11.dp, vertical = 5.dp),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
            )
        }
        Text(
            text = title,
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = subtitle,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

private val String.chapterLabel: String
    get() = when (this) {
        WishTraceTestTags.GiftDnaRedTile -> "THE PERSON"
        WishTraceTestTags.GiftDnaBlueTile -> "THE MOMENT"
        WishTraceTestTags.GiftDnaGreenTile -> "THEIR WORLD"
        else -> "YOUR CONTROL"
    }

@Composable
private fun SectionLabel(text: String, optional: Boolean = false) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
        )
        if (optional) {
            Text(
                text = "optional",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

@Composable
private fun <T> BentoRows(
    items: List<T>,
    content: @Composable (T, Modifier) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items.chunked(2).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                rowItems.forEach { item ->
                    content(item, Modifier.weight(1f))
                }
                if (rowItems.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun BentoChoice(
    title: String,
    icon: ImageVector,
    selected: Boolean,
    identity: TileIdentity,
    motionEnabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    minHeight: Dp = 108.dp,
) {
    val duration = if (motionEnabled) 180 else 0
    val scale by animateFloatAsState(
        targetValue = if (selected) 0.975f else 1f,
        animationSpec = if (motionEnabled) {
            spring(dampingRatio = 0.7f, stiffness = 520f)
        } else {
            tween(0)
        },
        label = "Bento choice scale",
    )
    val container by animateColorAsState(
        targetValue = if (selected) identity.selectedContainer else identity.soft,
        animationSpec = tween(duration),
        label = "Bento choice color",
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) identity.selectedContent else Ink,
        animationSpec = tween(duration),
        label = "Bento choice content",
    )

    Surface(
        onClick = onClick,
        modifier = modifier
            .heightIn(min = minHeight)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .semantics { this.selected = selected },
        color = container,
        contentColor = contentColor,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) identity.selectedContainer else identity.accent.copy(alpha = 0.13f),
        ),
        shadowElevation = if (selected) 3.dp else 0.dp,
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Surface(
                    color = if (selected) {
                        identity.selectedContent.copy(alpha = 0.16f)
                    } else {
                        Color.White.copy(alpha = 0.86f)
                    },
                    contentColor = if (selected) identity.selectedContent else identity.accent,
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.padding(8.dp),
                    )
                }
                AnimatedVisibility(
                    visible = selected,
                    enter = if (motionEnabled) {
                        fadeIn(tween(100)) + scaleIn(
                            initialScale = 0.55f,
                            animationSpec = spring(dampingRatio = 0.62f),
                        )
                    } else {
                        EnterTransition.None
                    },
                ) {
                    Surface(
                        color = identity.selectedContent,
                        contentColor = identity.selectedContainer,
                        shape = CircleShape,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = "Selected",
                            modifier = Modifier
                                .padding(4.dp)
                                .size(15.dp),
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            subtitle?.let {
                Text(
                    text = it,
                    color = contentColor.copy(alpha = 0.76f),
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
    }
}

@Composable
private fun GiftDnaActionBar(
    state: GiftDnaUiState,
    onAdvance: () -> Unit,
) {
    Surface(
        color = Color.White,
        shadowElevation = 8.dp,
    ) {
        PrimaryAction(
            text = when {
                state.saving -> "Saving"
                state.tile == GiftDnaTile.YELLOW -> "Save Gift DNA"
                state.tile == GiftDnaTile.GREEN && state.interests.isEmpty() -> "Skip for now"
                else -> "Continue"
            },
            onClick = onAdvance,
            enabled = !state.saving,
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 14.dp)
                .testTag(WishTraceTestTags.GiftDnaPrimaryAction),
            trailingContent = {
                if (state.saving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                }
            },
        )
    }
}

@Composable
private fun FieldError(text: String, color: Color) {
    Text(
        text = text,
        modifier = Modifier.padding(start = 12.dp),
        color = color,
        style = MaterialTheme.typography.bodyMedium,
    )
}

private fun giftDnaTransition(
    forward: Boolean,
    enabled: Boolean,
): ContentTransform {
    if (!enabled) return EnterTransition.None togetherWith ExitTransition.None
    val direction = if (forward) 1 else -1
    return (
        fadeIn(tween(170)) +
            slideInHorizontally(tween(250)) { width -> width * direction / 5 }
        ) togetherWith (
        fadeOut(tween(110)) +
            slideOutHorizontally(tween(180)) { width -> -width * direction / 8 }
        )
}

private val RelationshipChoice.icon: ImageVector
    get() = when (this) {
        RelationshipChoice.PARTNER -> Icons.Rounded.Favorite
        RelationshipChoice.FAMILY -> Icons.Rounded.Home
        RelationshipChoice.FRIEND -> Icons.Rounded.Group
        RelationshipChoice.COLLEAGUE -> Icons.Rounded.Work
    }

private val AgeBandChoice.icon: ImageVector
    get() = when (this) {
        AgeBandChoice.CHILD -> Icons.Rounded.ChildCare
        AgeBandChoice.TEEN -> Icons.Rounded.School
        AgeBandChoice.YOUNG_ADULT,
        AgeBandChoice.ADULT,
        -> Icons.Rounded.Person
        AgeBandChoice.SENIOR -> Icons.Rounded.Elderly
    }

private val AgeBandChoice.shortLabel: String
    get() = when (this) {
        AgeBandChoice.YOUNG_ADULT -> "Young adult"
        else -> label
    }

private val InterestChoice.icon: ImageVector
    get() = when (this) {
        InterestChoice.GAMING -> Icons.Rounded.SportsEsports
        InterestChoice.FITNESS -> Icons.Rounded.FitnessCenter
        InterestChoice.MUSIC -> Icons.Rounded.MusicNote
        InterestChoice.BOOKS -> Icons.AutoMirrored.Rounded.MenuBook
        InterestChoice.TECH -> Icons.Rounded.Devices
        InterestChoice.FOOD -> Icons.Rounded.Restaurant
    }

private val EnergyChoice.icon: ImageVector
    get() = when (this) {
        EnergyChoice.COMPETITIVE -> Icons.Rounded.EmojiEvents
        EnergyChoice.CHILL -> Icons.Rounded.Spa
    }

private fun Long.asWholeUsd(): String = "$${this / 100L}"
