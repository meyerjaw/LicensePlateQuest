package com.getmecookies.licenseplatequest.ui.screens.players

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.ui.draw.clip
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
import com.getmecookies.licenseplatequest.ui.PlayerColors
import com.getmecookies.licenseplatequest.ui.components.PlayerColorPicker
import com.getmecookies.licenseplatequest.ui.components.SwipeToDeleteRow
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
    onOpenSettings: () -> Unit = {},
    viewModel: PlayersViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.players_title)) },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            Icons.Filled.Settings,
                            contentDescription = stringResource(R.string.settings_title),
                        )
                    }
                },
            )
        },
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
                    onCommitDelete = viewModel::onSwipeDeleteCommit,
                )
            }
        }
    }

    when (val dialog = uiState.dialog) {
        PlayerDialog.None -> Unit

        is PlayerDialog.Edit -> EditPlayerDialog(
            name = dialog.name,
            colorToken = dialog.colorToken,
            error = dialog.error,
            onNameChange = viewModel::onDialogNameChange,
            onColorChange = viewModel::onDialogColorSelected,
            onConfirm = viewModel::onConfirmEdit,
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
    onCommitDelete: (Player) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(players, key = { it.player.id }) { item ->
            SwipeToDeleteRow(
                onDelete = { onCommitDelete(item.player) },
                deletedMessage = stringResource(R.string.players_deleted_snackbar, item.player.name),
                deleteContentDescription = stringResource(R.string.players_cd_delete, item.player.name),
                modifier = Modifier.animateItem(),
            ) {
                PlayerRow(item = item, onEdit = { onEdit(item.player) })
            }
        }
    }
}

@Composable
private fun PlayerRow(
    item: PlayerListItem,
    onEdit: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Decorative color dot; the player's name conveys identity for screen readers.
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(PlayerColors.resolve(item.player.color, item.player.id.toString())),
            )
            Spacer(Modifier.width(12.dp))
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
    colorToken: String?,
    error: PlayerNameError?,
    onNameChange: (String) -> Unit,
    onColorChange: (String) -> Unit,
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
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = onNameChange,
                    singleLine = true,
                    label = { Text(stringResource(R.string.players_name_label)) },
                    isError = errorText != null,
                    supportingText = errorText?.let { { Text(it) } },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                )
                Text(
                    text = stringResource(R.string.player_color_label),
                    style = MaterialTheme.typography.titleSmall,
                )
                PlayerColorPicker(
                    selectedToken = colorToken,
                    onSelect = onColorChange,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = { TextButton(onClick = onConfirm) { Text(stringResource(R.string.action_save)) } },
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
