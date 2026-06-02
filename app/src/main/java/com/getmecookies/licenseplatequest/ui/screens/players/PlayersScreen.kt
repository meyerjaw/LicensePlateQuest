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
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.getmecookies.licenseplatequest.R
import com.getmecookies.licenseplatequest.domain.model.Player
import com.getmecookies.licenseplatequest.domain.model.PlayerListItem
import com.getmecookies.licenseplatequest.ui.AppViewModelProvider
import com.getmecookies.licenseplatequest.ui.theme.LicensePlateQuestTheme
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
        topBar = { TopAppBar(title = { Text(stringResource(R.string.players_title)) }) },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddPlayer) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.players_cd_add))
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
        text = stringResource(R.string.players_empty),
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
                Icon(
                    Icons.Filled.Edit,
                    contentDescription = stringResource(R.string.players_cd_edit, item.player.name),
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = stringResource(R.string.players_cd_delete, item.player.name),
                )
            }
        }
    }
}

/** "3 trips - last played May 12, 2026" — or a friendly note when never played. */
@Composable
private fun playStatsLabel(item: PlayerListItem): String {
    val tripWord = if (item.tripCount == 1) {
        stringResource(R.string.players_trip_singular)
    } else {
        stringResource(R.string.players_trip_plural)
    }
    val plays = stringResource(R.string.players_trip_count, item.tripCount, tripWord)
    val last = item.lastPlayed
    return if (last == null) {
        stringResource(R.string.players_stats_not_played, plays)
    } else {
        stringResource(R.string.players_stats_last_played, plays, last.format(DATE_FORMAT))
    }
}

private val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy")

@Composable
private fun EditPlayerDialog(
    name: String,
    error: PlayerNameError?,
    onNameChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val errorText = when (error) {
        PlayerNameError.BLANK -> stringResource(R.string.player_name_blank)
        PlayerNameError.DUPLICATE ->
            stringResource(R.string.player_name_duplicate, name.trim())
        null -> null
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.players_edit_title)) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = onNameChange,
                singleLine = true,
                label = { Text(stringResource(R.string.players_name_label)) },
                isError = errorText != null,
                supportingText = errorText?.let { { Text(it) } },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
            )
        },
        confirmButton = { TextButton(onClick = onConfirm) { Text(stringResource(R.string.action_save)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
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
        title = { Text(stringResource(R.string.players_delete_title, player.name)) },
        text = {
            val message = if (tripCount > 0) {
                val tripWord = if (tripCount == 1) {
                    stringResource(R.string.players_trip_singular)
                } else {
                    stringResource(R.string.players_trip_plural)
                }
                stringResource(R.string.players_delete_on_trips, player.name, tripCount, tripWord)
            } else {
                stringResource(R.string.players_delete_no_trips, player.name)
            }
            Text(message)
        },
        confirmButton = { TextButton(onClick = onConfirm) { Text(stringResource(R.string.action_delete)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

@Preview(showBackground = true)
@Composable
private fun PlayersScreenPreview() {
    LicensePlateQuestTheme {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.players_empty))
        }
    }
}
