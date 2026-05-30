package com.getmecookies.licenseplatequest.ui.screens.trips

import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.getmecookies.licenseplatequest.domain.model.TripListItem
import com.getmecookies.licenseplatequest.ui.AppViewModelProvider
import java.time.format.DateTimeFormatter

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
    viewModel: TripListViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Trips") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = onNewTrip) {
                Icon(Icons.Filled.Add, contentDescription = "New trip")
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
                    text = "No trips yet — tap + to start your first one.",
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
                        viewModel.onSelectTrip(item.id)
                        onOpenTrip()
                    },
                    onDelete = viewModel::onDeleteRequest,
                )
            }
        }
    }

    uiState.deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = viewModel::onDismissDelete,
            title = { Text("Delete trip?") },
            text = { Text("Delete \"${target.name}\"? This can't be undone.") },
            confirmButton = { TextButton(onClick = viewModel::onConfirmDelete) { Text("Delete") } },
            dismissButton = { TextButton(onClick = viewModel::onDismissDelete) { Text("Cancel") } },
        )
    }
}

@Composable
private fun TripSections(
    uiState: TripListUiState,
    onSelect: (TripListItem) -> Unit,
    onDelete: (TripListItem) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        uiState.active?.let { active ->
            item(key = "header-active") { SectionHeader("Active") }
            item(key = active.id) {
                TripRow(item = active, onSelect = onSelect, onDelete = onDelete)
            }
        }

        if (uiState.inProgress.isNotEmpty()) {
            item(key = "header-in-progress") { SectionHeader("In Progress") }
            items(uiState.inProgress, key = { it.id }) { item ->
                TripRow(item = item, onSelect = onSelect, onDelete = onDelete)
            }
        }

        if (uiState.completed.isNotEmpty()) {
            item(key = "header-completed") { SectionHeader("Completed") }
            items(uiState.completed, key = { it.id }) { item ->
                TripRow(item = item, onSelect = onSelect, onDelete = onDelete)
            }
        }
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

    Card(
        colors = colors,
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { onSelect(item) },
                onLongClick = { onDelete(item) },
            ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                if (item.isComplete) {
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = "All 50 states found",
                        tint = MaterialTheme.colorScheme.tertiary,
                    )
                }
            }

            Text(
                text = "Started ${item.startDate.format(DATE_FORMAT)}",
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
                text = "${item.foundCount} / ${TripListItem.TOTAL_STATES} states",
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

private val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy")
