package com.getmecookies.licenseplatequest.ui.screens.manageplayers

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.getmecookies.licenseplatequest.domain.model.Player
import com.getmecookies.licenseplatequest.ui.AppViewModelProvider

/**
 * Manage the players on a trip (reached from the Active Trip overflow menu). Lets the user add
 * a brand-new player, add players already in their roster, and remove players from the trip.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManagePlayersScreen(
    onBack: () -> Unit,
    viewModel: ManagePlayersViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Players") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { innerPadding ->
        if (uiState.loading) {
            Column(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            AddNewPlayerCard(
                name = uiState.newName,
                error = uiState.newNameError,
                adding = uiState.addingNew,
                onNameChange = viewModel::onNewNameChange,
                onAdd = viewModel::onAddNew,
            )

            Section(title = "On this trip (${uiState.onTrip.size})") {
                if (uiState.onTrip.isEmpty()) {
                    HintText("No players on this trip yet. Add some below.")
                } else {
                    uiState.onTrip.forEachIndexed { index, player ->
                        if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        PlayerRow(
                            player = player,
                            actionIcon = Icons.Filled.Close,
                            actionDescription = "Remove ${player.name} from this trip",
                            onAction = { viewModel.onRemove(player.id) },
                        )
                    }
                }
            }

            if (uiState.available.isNotEmpty()) {
                Section(title = "Add from your players") {
                    uiState.available.forEachIndexed { index, player ->
                        if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        PlayerRow(
                            player = player,
                            actionIcon = Icons.Filled.Add,
                            actionDescription = "Add ${player.name} to this trip",
                            onAction = { viewModel.onAddExisting(player.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AddNewPlayerCard(
    name: String,
    error: String?,
    adding: Boolean,
    onNameChange: (String) -> Unit,
    onAdd: () -> Unit,
) {
    Section(title = "Add a new player") {
        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Player name") },
            singleLine = true,
            isError = error != null,
            supportingText = error?.let { msg -> { Text(msg) } },
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(onDone = { onAdd() }),
        )
        Button(
            onClick = onAdd,
            enabled = !adding && name.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Add player")
        }
    }
}

@Composable
private fun PlayerRow(
    player: Player,
    actionIcon: androidx.compose.ui.graphics.vector.ImageVector,
    actionDescription: String,
    onAction: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = player.name,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onAction) {
            Icon(
                imageVector = actionIcon,
                contentDescription = actionDescription,
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

/** A titled card grouping related rows, matching the rest of the app's section styling. */
@Composable
private fun Section(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
        )
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                content = content,
            )
        }
    }
}

@Composable
private fun HintText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
