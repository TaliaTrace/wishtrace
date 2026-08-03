package com.wishtrace.app.ui.screens.occasions

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.wishtrace.app.domain.HomeSnapshot
import com.wishtrace.app.ui.HomeUiState
import com.wishtrace.app.ui.components.PrimaryAction
import com.wishtrace.app.ui.components.RecipientAvatar
import com.wishtrace.app.ui.theme.BrandIndigo
import com.wishtrace.app.ui.theme.BlueSurface
import com.wishtrace.app.ui.theme.Ink
import com.wishtrace.app.ui.theme.InkMuted
import com.wishtrace.app.ui.theme.LavenderSurface
import com.wishtrace.app.ui.theme.OutlineCool
import com.wishtrace.app.ui.theme.SurfaceWhite
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun OccasionsScreen(
    state: HomeUiState,
    onRetry: () -> Unit,
    onOpenOccasion: () -> Unit,
    onAddOccasion: () -> Unit,
    onEditOccasion: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (state) {
        HomeUiState.Loading -> Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator()
        }

        HomeUiState.Empty -> EmptyOccasions(onAddOccasion, modifier)
        is HomeUiState.Error -> ErrorOccasions(state.message, onRetry, modifier)
        is HomeUiState.Content -> OccasionsContent(
            snapshot = state.snapshot,
            onOpenOccasion = onOpenOccasion,
            onEditOccasion = onEditOccasion,
            modifier = modifier,
        )
    }
}

@Composable
private fun OccasionsContent(
    snapshot: HomeSnapshot,
    onOpenOccasion: () -> Unit,
    onEditOccasion: () -> Unit,
    modifier: Modifier,
) {
    val occasionDate = snapshot.occasion.localDate
    val initialMonth = YearMonth.from(occasionDate)
    var monthText by rememberSaveable { mutableStateOf(initialMonth.toString()) }
    var selectedText by rememberSaveable { mutableStateOf(occasionDate.toString()) }
    val month = YearMonth.parse(monthText)
    val selectedDate = LocalDate.parse(selectedText)

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
                    text = "Occasions",
                    modifier = Modifier.semantics { heading() },
                    style = MaterialTheme.typography.headlineLarge,
                )
                TextButton(onClick = onEditOccasion) {
                    Text(text = "Edit")
                }
            }
        }
        item {
            UpcomingOccasionBento(
                snapshot = snapshot,
                onClick = onOpenOccasion,
            )
        }
        item {
            MonthCalendar(
                month = month,
                selectedDate = selectedDate,
                eventDate = occasionDate,
                eventLabel = "${snapshot.recipient.displayName}'s ${snapshot.occasion.kind.displayName}",
                onPreviousMonth = {
                    monthText = month.minusMonths(1).toString()
                },
                onNextMonth = {
                    monthText = month.plusMonths(1).toString()
                },
                onSelectDate = { selectedText = it.toString() },
            )
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = if (selectedDate == occasionDate) {
                        "On ${selectedDate.format(DateTimeFormatter.ofPattern("MMM d", Locale.US))}"
                    } else {
                        "Upcoming"
                    },
                    style = MaterialTheme.typography.titleSmall,
                )
                OccasionAgendaCard(
                    snapshot = snapshot,
                    onClick = onOpenOccasion,
                )
            }
        }
    }
}

@Composable
private fun UpcomingOccasionBento(
    snapshot: HomeSnapshot,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(132.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Surface(
            onClick = onClick,
            modifier = Modifier
                .weight(1.45f)
                .fillMaxHeight(),
            color = LavenderSurface,
            contentColor = Ink,
            shape = RoundedCornerShape(26.dp),
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RecipientAvatar(
                    initials = snapshot.recipient.initials,
                    photoUri = snapshot.recipient.photoUri,
                    size = 56.dp,
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Text(
                        text = "NEXT",
                        color = BrandIndigo,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = snapshot.recipient.displayName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                    )
                    Text(
                        text = snapshot.occasion.kind.displayName,
                        color = InkMuted,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
        Surface(
            modifier = Modifier
                .weight(0.82f)
                .fillMaxHeight(),
            color = BrandIndigo,
            contentColor = SurfaceWhite,
            shape = RoundedCornerShape(26.dp),
        ) {
            Column(
                modifier = Modifier.padding(15.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "COUNTDOWN",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = snapshot.daysUntil.toString(),
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(text = "days", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun MonthCalendar(
    month: YearMonth,
    selectedDate: LocalDate,
    eventDate: LocalDate,
    eventLabel: String,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onSelectDate: (LocalDate) -> Unit,
) {
    val leadingEmptyCells = month.atDay(1).dayOfWeek.value - 1
    val cells = buildList<LocalDate?> {
        repeat(leadingEmptyCells) { add(null) }
        repeat(month.lengthOfMonth()) { day -> add(month.atDay(day + 1)) }
        while (size % 7 != 0) add(null)
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = BlueSurface,
        contentColor = Ink,
        shape = RoundedCornerShape(30.dp),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = onPreviousMonth,
                    modifier = Modifier.semantics { contentDescription = "Previous month" },
                ) {
                    Icon(Icons.Rounded.ChevronLeft, contentDescription = null)
                }
                Surface(
                    color = SurfaceWhite,
                    contentColor = Ink,
                    shape = CircleShape,
                ) {
                    Text(
                        text = month.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.US)),
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                }
                IconButton(
                    onClick = onNextMonth,
                    modifier = Modifier.semantics { contentDescription = "Next month" },
                ) {
                    Icon(Icons.Rounded.ChevronRight, contentDescription = null)
                }
            }
            Row(modifier = Modifier.fillMaxWidth()) {
                listOf("M", "T", "W", "T", "F", "S", "S").forEach { day ->
                    Text(
                        text = day,
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
            cells.chunked(7).forEach { week ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    week.forEach { date ->
                        if (date == null) {
                            Spacer(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp),
                            )
                        } else {
                            CalendarDay(
                                date = date,
                                selected = date == selectedDate,
                                hasEvent = date == eventDate,
                                eventLabel = eventLabel,
                                onClick = { onSelectDate(date) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarDay(
    date: LocalDate,
    selected: Boolean,
    hasEvent: Boolean,
    eventLabel: String,
    onClick: () -> Unit,
    modifier: Modifier,
) {
    val description = buildString {
        append(date.format(DateTimeFormatter.ofPattern("MMMM d", Locale.US)))
        if (hasEvent) append(", $eventLabel")
        if (selected) append(", selected")
    }
    Box(
        modifier = modifier.height(50.dp),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            onClick = onClick,
            modifier = Modifier
                .size(46.dp)
                .semantics {
                    contentDescription = description
                    role = Role.Button
                },
            color = if (selected) BrandIndigo else Color.Transparent,
            contentColor = if (selected) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                Ink
            },
            shape = CircleShape,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = date.dayOfMonth.toString(),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (hasEvent) FontWeight.Bold else FontWeight.Medium,
                )
                if (hasEvent) {
                    Box(
                        modifier = Modifier
                            .padding(top = 2.dp)
                            .size(4.dp),
                    ) {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = if (selected) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                BrandIndigo
                            },
                            shape = CircleShape,
                        ) {}
                    }
                }
            }
        }
    }
}

@Composable
private fun OccasionAgendaCard(
    snapshot: HomeSnapshot,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        color = SurfaceWhite,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, OutlineCool),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            RecipientAvatar(
                initials = snapshot.recipient.initials,
                photoUri = snapshot.recipient.photoUri,
                size = 48.dp,
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = "${snapshot.recipient.displayName}'s ${snapshot.occasion.kind.displayName}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "${snapshot.daysUntil} days · ${snapshot.occasion.budget.formatted(Locale.US)} budget",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Icon(
                imageVector = Icons.Rounded.CardGiftcard,
                contentDescription = "Open occasion",
                tint = BrandIndigo,
            )
        }
    }
}

@Composable
private fun EmptyOccasions(onAddOccasion: () -> Unit, modifier: Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = "No dates yet", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Add the next occasion you want to remember.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(20.dp))
        PrimaryAction(
            text = "Add an occasion",
            onClick = onAddOccasion,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun ErrorOccasions(message: String, onRetry: () -> Unit, modifier: Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = "Occasions could not load.", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = message, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(20.dp))
        PrimaryAction(
            text = "Try again",
            onClick = onRetry,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
