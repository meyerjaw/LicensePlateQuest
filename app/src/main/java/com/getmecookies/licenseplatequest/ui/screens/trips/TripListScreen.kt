package com.getmecookies.licenseplatequest.ui.screens.trips

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.getmecookies.licenseplatequest.R
import com.getmecookies.licenseplatequest.domain.model.TripListItem
import com.getmecookies.licenseplatequest.domain.model.TripStatus
import com.getmecookies.licenseplatequest.ui.AppViewModelProvider
import com.getmecookies.licenseplatequest.ui.components.SwipeToDeleteRow
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.UUID

/**
 * Trip List / Home (SPEC section 6). Trips are grouped into Active, In Progress, and
 * Completed sections. Each row shows the trip name, status, X / 50 progress, and start date;
 * completed-map trips (all 50 found) get a star + accent border. Tapping a row makes that
 * trip active; long-pressing asks to delete. The FAB opens the full-screen New Trip flow.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripListScreen(
    onNewTrip: () -> Unit,
    onOpenTrip: () -> Unit,
    onOpenSummary: (UUID) -> Unit = {},
    viewModel: TripListViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.trip_list_title)) }) },
        floatingActionButton = {
            FloatingActionButton(onClick = onNewTrip) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.trip_list_cd_new_trip))
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            if (!uiState.loading && uiState.isEmpty) {
                Text(
                    text = stringResource(R.string.trip_list_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp),
                )
            } else {
                TripSections(
                    uiState = uiState,
                    onSelect = { item ->
                        // A completed trip is finished — re-open its read-only summary instead
                        // of reactivating it; others activate and open the active trip view.
                        if (item.status == TripStatus.COMPLETED) {
                            onOpenSummary(item.id)
                        } else {
                            viewModel.onSelectTrip(item.id)
                            onOpenTrip()
                        }
                    },
                    onDelete = viewModel::onDeleteRequest,
                    onCommitDelete = viewModel::onSwipeDeleteCommit,
                )
            }
        }
    }

    uiState.deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = viewModel::onDismissDelete,
            title = { Text(stringResource(R.string.trip_list_delete_title)) },
            text = { Text(stringResource(R.string.trip_list_delete_body, target.name)) },
            confirmButton = { TextButton(onClick = viewModel::onConfirmDelete) { Text(stringResource(R.string.action_delete)) } },
            dismissButton = { TextButton(onClick = viewModel::onDismissDelete) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}

@Composable
private fun TripSections(
    uiState: TripListUiState,
    onSelect: (TripListItem) -> Unit,
    onDelete: (TripListItem) -> Unit,
    onCommitDelete: (TripListItem) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        uiState.active?.let { active ->
            item(key = "header-active") { SectionHeader(stringResource(R.string.trip_list_section_active)) }
            item(key = active.id) {
                SwipeableTripRow(active, onSelect, onDelete, onCommitDelete, Modifier.animateItem())
            }
        }

        if (uiState.inProgress.isNotEmpty()) {
            item(key = "header-in-progress") { SectionHeader(stringResource(R.string.trip_list_section_in_progress)) }
            items(uiState.inProgress, key = { it.id }) { item ->
                SwipeableTripRow(item, onSelect, onDelete, onCommitDelete, Modifier.animateItem())
            }
        }

        if (uiState.completed.isNotEmpty()) {
            item(key = "header-completed") { SectionHeader(stringResource(R.string.trip_list_section_completed)) }
            items(uiState.completed, key = { it.id }) { item ->
                SwipeableTripRow(item, onSelect, onDelete, onCommitDelete, Modifier.animateItem())
            }
        }
    }
}

/**
 * A trip row that can be swiped (either direction) to delete. The swipe flips the row into an
 * in-place undo bar for a few seconds (handled by [SwipeToDeleteRow]); only if it isn't undone
 * does [onCommitDelete] fire to perform the real deletion.
 */
@Composable
private fun SwipeableTripRow(
    item: TripListItem,
    onSelect: (TripListItem) -> Unit,
    onDelete: (TripListItem) -> Unit,
    onCommitDelete: (TripListItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    SwipeToDeleteRow(
        onDelete = { onCommitDelete(item) },
        deletedMessage = stringResource(R.string.trip_list_deleted_snackbar, item.name),
        deleteContentDescription = stringResource(R.string.trip_list_cd_delete),
        modifier = modifier,
    ) {
        TripRow(item = item, onSelect = onSelect, onDelete = onDelete)
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp),
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TripRow(
    item: TripListItem,
    onSelect: (TripListItem) -> Unit,
    onDelete: (TripListItem) -> Unit,
) {
    // Completed-map trips (all 50 found) stand out with a star and accent container.
    val colors = if (item.isComplete) {
        CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
    } else {
        CardDefaults.cardColors()
    }
    // A gold accent outline reinforces a completed-map trip beyond color alone.
    val cardModifier = Modifier
        .fillMaxWidth()
        .then(
            if (item.isComplete) {
                Modifier.border(
                    width = 1.5.dp,
                    color = MaterialTheme.colorScheme.tertiary,
                    shape = CardDefaults.shape,
                )
            } else {
                Modifier
            },
        )
        .combinedClickable(
            onClick = { onSelect(item) },
            onLongClick = { onDelete(item) },
        )

    Card(colors = colors, modifier = cardModifier) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                StatusChip(item.status)
                if (item.isComplete) {
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = stringResource(R.string.trip_list_cd_completed),
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.padding(start = 6.dp),
                    )
                }
            }

            val startLabel = relativeStartLabel(item.startDate)
            val subtitle = item.durationLabel
                ?.let { stringResource(R.string.trip_list_started_lasted, startLabel, it) }
                ?: stringResource(R.string.trip_list_started, startLabel)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp),
            )

            LinearProgressIndicator(
                progress = { item.foundCount.toFloat() / TripListItem.TOTAL_STATES },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
                    .clip(RoundedCornerShape(8.dp)),
            )
            Text(
                text = stringResource(
                    R.string.trip_list_states_count,
                    item.foundCount,
                    TripListItem.TOTAL_STATES,
                ),
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

/** Small pill showing a trip's lifecycle status, color-coded. */
@Composable
private fun StatusChip(status: TripStatus) {
    val (label, container, content) = when (status) {
        TripStatus.ACTIVE -> Triple(
            stringResource(R.string.trip_list_status_active),
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.onPrimary,
        )
        TripStatus.IN_PROGRESS -> Triple(
            stringResource(R.string.trip_list_status_in_progress),
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer,
        )
        TripStatus.COMPLETED -> Triple(
            stringResource(R.string.trip_list_status_completed),
            MaterialTheme.colorScheme.tertiary,
            MaterialTheme.colorScheme.onTertiary,
        )
    }
    Surface(color = container, contentColor = content, shape = RoundedCornerShape(50)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
        )
    }
}

private val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy")

/**
 * Friendly relative phrasing for a trip's start date: "today", "yesterday", "N days ago" for
 * the last week, otherwise the absolute date. Future dates fall back to the absolute date.
 */
@Composable
private fun relativeStartLabel(date: LocalDate): String {
    val today = LocalDate.now()
    val days = ChronoUnit.DAYS.between(date, today)
    return when {
        days == 0L -> stringResource(R.string.trip_list_relative_today)
        days == 1L -> stringResource(R.string.trip_list_relative_yesterday)
        days in 2L..6L -> stringResource(R.string.trip_list_relative_days_ago, days.toInt())
        else -> date.format(DATE_FORMAT)
    }
}
