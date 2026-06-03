package com.getmecookies.licenseplatequest.ui.screens.trips

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.getmecookies.licenseplatequest.R
import com.getmecookies.licenseplatequest.domain.model.RegionOption
import com.getmecookies.licenseplatequest.ui.AppViewModelProvider
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID

/**
 * Full-screen New Trip form (SPEC sections 6/7), mirroring the Add Player pattern: reached
 * from the Trip List FAB, not a dialog. Collects name (auto-prefilled), origin/destination
 * city + state, start date, and players. "+ Add new" reuses the shared full-screen Add
 * Player flow; the created player is reported back via [addedPlayerId] and auto-selected.
 * Saving creates an ACTIVE trip and returns.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun NewTripScreen(
    onDone: () -> Unit,
    onAddPlayer: () -> Unit,
    addedPlayerId: String? = null,
    onAddedPlayerConsumed: () -> Unit = {},
    viewModel: NewTripViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val saved by viewModel.saved.collectAsStateWithLifecycle()

    LaunchedEffect(saved) {
        if (saved) onDone()
    }

    // Auto-select a player just created via the shared Add Player screen, then clear the
    // one-shot navigation result so it isn't re-applied on recomposition.
    LaunchedEffect(addedPlayerId) {
        if (addedPlayerId != null) {
            viewModel.onExternalPlayerAdded(UUID.fromString(addedPlayerId))
            onAddedPlayerConsumed()
        }
    }

    var showDatePicker by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.new_trip_title)) },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            OutlinedTextField(
                value = uiState.name,
                onValueChange = viewModel::onNameChange,
                label = { Text(stringResource(R.string.new_trip_name_label)) },
                singleLine = true,
                isError = uiState.showErrors && !uiState.nameValid,
                supportingText = if (uiState.showErrors && !uiState.nameValid) {
                    { Text(stringResource(R.string.new_trip_name_error)) }
                } else null,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                modifier = Modifier.fillMaxWidth(),
            )

            // --- Origin -------------------------------------------------
            Text(stringResource(R.string.new_trip_from), style = MaterialTheme.typography.titleSmall)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = uiState.originCity,
                    onValueChange = viewModel::onOriginCityChange,
                    label = { Text(stringResource(R.string.new_trip_city_label)) },
                    singleLine = true,
                    isError = uiState.showErrors && !uiState.originCityValid,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Next,
                    ),
                    modifier = Modifier.weight(1f),
                )
                StateDropdown(
                    label = stringResource(R.string.new_trip_state_label),
                    options = uiState.regionOptions,
                    selected = uiState.originRegion,
                    isError = uiState.showErrors && !uiState.originRegionValid,
                    onSelected = viewModel::onOriginRegionSelected,
                    modifier = Modifier.width(130.dp),
                )
                // Always-present clear ✕ so the row never reflows; disabled when nothing's set
                // (playtest note #9).
                IconButton(
                    onClick = viewModel::onClearOrigin,
                    enabled = uiState.hasOrigin,
                ) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = stringResource(R.string.new_trip_clear_origin_cd),
                    )
                }
            }

            // --- Destination --------------------------------------------
            Text(stringResource(R.string.new_trip_to), style = MaterialTheme.typography.titleSmall)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = uiState.destinationCity,
                    onValueChange = viewModel::onDestinationCityChange,
                    label = { Text(stringResource(R.string.new_trip_city_label)) },
                    singleLine = true,
                    isError = uiState.showErrors && !uiState.destinationCityValid,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Next,
                    ),
                    modifier = Modifier.weight(1f),
                )
                StateDropdown(
                    label = stringResource(R.string.new_trip_state_label),
                    options = uiState.regionOptions,
                    selected = uiState.destinationRegion,
                    isError = uiState.showErrors && !uiState.destinationRegionValid,
                    onSelected = viewModel::onDestinationRegionSelected,
                    modifier = Modifier.width(130.dp),
                )
                IconButton(
                    onClick = viewModel::onClearDestination,
                    enabled = uiState.hasDestination,
                ) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = stringResource(R.string.new_trip_clear_destination_cd),
                    )
                }
            }

            // --- Start date ---------------------------------------------
            Text(stringResource(R.string.new_trip_start_date), style = MaterialTheme.typography.titleSmall)
            AssistChip(
                onClick = { showDatePicker = true },
                label = { Text(uiState.startDate.format(DATE_FORMAT)) },
            )

            HorizontalDivider()

            // --- Players ------------------------------------------------
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(stringResource(R.string.new_trip_players), style = MaterialTheme.typography.titleSmall)
                TextButton(onClick = onAddPlayer) { Text(stringResource(R.string.new_trip_add_new)) }
            }
            if (uiState.showErrors && !uiState.playersValid) {
                Text(
                    stringResource(R.string.new_trip_players_error),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            if (uiState.allPlayers.isEmpty()) {
                Text(
                    stringResource(R.string.new_trip_no_players),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    uiState.allPlayers.forEach { player ->
                        FilterChip(
                            selected = player.id in uiState.selectedPlayerIds,
                            onClick = { viewModel.onTogglePlayer(player.id) },
                            label = { Text(player.name) },
                        )
                    }
                }
            }

            Button(
                onClick = viewModel::onSave,
                enabled = !uiState.saving,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
            ) {
                Text(stringResource(R.string.new_trip_start))
            }
        }
    }

    if (showDatePicker) {
        TripDatePicker(
            initial = uiState.startDate,
            onPicked = {
                viewModel.onStartDateChange(it)
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false },
        )
    }
}

@Composable
private fun StateDropdown(
    label: String,
    options: List<RegionOption>,
    selected: RegionOption?,
    isError: Boolean,
    onSelected: (UUID) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        OutlinedTextField(
            value = selected?.code ?: "",
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            isError = isError,
            trailingIcon = { Icon(Icons.Filled.ArrowDropDown, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
        )
        // A readOnly text field doesn't reliably receive clicks, so an invisible overlay
        // opens the menu when tapped.
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable { expanded = true },
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.new_trip_region_option, option.code, option.name)) },
                    onClick = {
                        onSelected(option.id)
                        expanded = false
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TripDatePicker(
    initial: LocalDate,
    onPicked: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
) {
    val initialMillis = initial.atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()
    val state = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                val millis = state.selectedDateMillis
                if (millis != null) {
                    val date = Instant.ofEpochMilli(millis).atZone(ZoneId.of("UTC")).toLocalDate()
                    onPicked(date)
                } else {
                    onDismiss()
                }
            }) { Text(stringResource(R.string.action_ok)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    ) {
        DatePicker(state = state)
    }
}

private val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy")
