package com.getmecookies.licenseplatequest.ui.screens.players

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.getmecookies.licenseplatequest.domain.model.Player
import com.getmecookies.licenseplatequest.domain.model.PlayerListItem
import com.getmecookies.licenseplatequest.ui.AppViewModelProvider
import com.getmecookies.licenseplatequest.ui.theme.LicensePlateQuestTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Players roster (SPEC section 6). The FAB opens the full-screen Add Player flow; tapping a
 * row edits the name; long-pressing (or the trash icon) deletes with confirmation — warning
 * when the player is already on trips. Each row also shows trip-based play stats.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayersScreen(
    onAddPlayer: () -> Unit,
    viewModel: PlayersViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Players") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddPlayer) {
                Icon(Icons.Filled.Add, contentDescription = "Add player")
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            if (!uiState.loading && uiState.players.isEmpty()) {
                EmptyPlayers(Modifier.align(Alignment.Center))
            } else {
                PlayerList(
                    players = uiState.players,
                    onEdit = viewModel::onEditClick,
                    onDelete = viewModel::onDeleteClick,
                )
            }
        }
    }

    when (val dialog = uiState.dialog) {
        PlayerDialog.None -> Unit

        is PlayerDialog.Edit -> EditPlayerDialog(
            name = dialog.name,
            error = dialog.error,
            onNameChange = viewModel::onDialogNameChange,
            onConfirm = viewModel::onConfirmEdit,
            onDismiss = viewModel::onDismissDialog,
        )

        is PlayerDialog.ConfirmDelete -> DeletePlayerDialog(
            player = dialog.player,
            tripCount = dialog.tripCount,
            onConfirm = viewModel::onConfirmDelete,
            onDismiss = viewModel::onDismissDialog,
        )
    }
}

@Composable
private fun EmptyPlayers(modifier: Modifier = Modifier) {
    Text(
        text = "Add your first player to get started.",
        style = MaterialTheme.typography.bodyLarge,
        textAlign = TextAlign.Center,
        modifier = modifier.padding(24.dp),
    )
}

@Composable
private fun PlayerList(
    players: List<PlayerListItem>,
    onEdit: (Player) -> Unit,
    onDelete: (Player) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(players, key = { it.player.id }) { item ->
            PlayerRow(
                item = item,
                onEdit = { onEdit(item.player) },
                onDelete = { onDelete(item.player) },
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PlayerRow(
    item: PlayerListItem,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onEdit,
                onLongClick = onDelete,
            ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.player.name,
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    text = playStatsLabel(item),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Filled.Edit, contentDescription = "Edit ${item.player.name}")
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete ${item.player.name}")
            }
        }
    }
}

/** "3 trips - last played May 12, 2026" — or a friendly note when never played. */
private fun playStatsLabel(item: PlayerListItem): String {
    val tripWord = if (item.tripCount == 1) "trip" else "trips"
    val plays = "${item.tripCount} $tripWord"
    val last = item.lastPlayed
    return if (last == null) {
        "$plays - not played yet"
    } else {
        "$plays - last played ${last.format(DATE_FORMAT)}"
    }
}

private val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy")

@Composable
private fun EditPlayerDialog(
    name: String,
    error: String?,
    onNameChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit name") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = onNameChange,
                singleLine = true,
                label = { Text("Name") },
                isError = error != null,
                supportingText = error?.let { { Text(it) } },
            )
        },
        confirmButton = { TextButton(onClick = onConfirm) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun DeletePlayerDialog(
    player: Player,
    tripCount: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete ${player.name}?") },
        text = {
            val message = if (tripCount > 0) {
                val tripWord = if (tripCount == 1) "trip" else "trips"
                "${player.name} is on $tripCount $tripWord. Deleting keeps that trip history " +
                    "but removes them from the roster."
            } else {
                "This removes ${player.name} from your roster."
            }
            Text(message)
        },
        confirmButton = { TextButton(onClick = onConfirm) { Text("Delete") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Preview(showBackground = true)
@Composable
private fun PlayersScreenPreview() {
    LicensePlateQuestTheme {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Add your first player to get started.")
        }
    }
}
