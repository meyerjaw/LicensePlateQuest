package com.getmecookies.licenseplatequest.ui.screens.trips

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.getmecookies.licenseplatequest.ui.components.rememberNotificationPermissionPrimer
import com.getmecookies.licenseplatequest.domain.model.RegionOption
import com.getmecookies.licenseplatequest.ui.AppViewModelProvider
import com.getmecookies.licenseplatequest.ui.PlayerColors
import com.getmecookies.licenseplatequest.ui.components.PlayerSelectChip
import com.getmecookies.licenseplatequest.ui.components.RegionPickerField
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
    var showEndDatePicker by remember { mutableStateOf(false) }

    // Setting an end date schedules an overdue reminder; offer notification permission via the
    // pre-permission primer the first time one is picked (Android 13+). Optional — trip still saves.
    val notificationPrimer = rememberNotificationPermissionPrimer()

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
            // --- Stops (the route: start → … → destination) -------------
            Text(stringResource(R.string.new_trip_stops), style = MaterialTheme.typography.titleSmall)
            uiState.stops.forEachIndexed { index, stop ->
                StopEditor(
                    index = index,
                    total = uiState.stops.size,
                    city = stop.city,
                    regionId = stop.regionId,
                    regionOptions = uiState.regionOptions,
                    isError = uiState.showErrors && !uiState.stopValid(index),
                    onCityChange = { viewModel.onStopCityChange(index, it) },
                    onRegionSelected = { viewModel.onStopRegionSelected(index, it) },
                    onMoveUp = { viewModel.onMoveStopUp(index) },
                    onMoveDown = { viewModel.onMoveStopDown(index) },
                    onRemove = { viewModel.onRemoveStop(index) },
                )
            }
            if (uiState.showErrors && !uiState.stopsValid) {
                Text(
                    stringResource(R.string.new_trip_stops_error),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            TextButton(onClick = viewModel::onAddStop) {
                Text(stringResource(R.string.new_trip_add_stop))
            }

            // --- Trip name (auto-fills from the stops above) ------------
            Text(
                stringResource(R.string.new_trip_name_label),
                style = MaterialTheme.typography.titleSmall,
            )
            OutlinedTextField(
                value = uiState.name,
                onValueChange = viewModel::onNameChange,
                placeholder = { Text(stringResource(R.string.new_trip_name_placeholder)) },
                singleLine = true,
                isError = uiState.showErrors && !uiState.nameValid,
                supportingText = if (uiState.showErrors && !uiState.nameValid) {
                    { Text(stringResource(R.string.new_trip_name_error)) }
                } else null,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                trailingIcon = if (uiState.name.isNotEmpty()) {
                    {
                        IconButton(onClick = viewModel::onClearName) {
                            Icon(
                                Icons.Filled.Close,
                                contentDescription = stringResource(R.string.new_trip_clear_name_cd),
                            )
                        }
                    }
                } else {
                    null
                },
                modifier = Modifier.fillMaxWidth(),
            )

            // --- Start date ---------------------------------------------
            Text(stringResource(R.string.new_trip_start_date), style = MaterialTheme.typography.titleSmall)
            AssistChip(
                onClick = { showDatePicker = true },
                label = { Text(uiState.startDate.format(DATE_FORMAT)) },
            )

            // --- End date (optional) ------------------------------------
            Text(stringResource(R.string.new_trip_end_date), style = MaterialTheme.typography.titleSmall)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AssistChip(
                    onClick = { showEndDatePicker = true },
                    label = {
                        Text(
                            uiState.endDate?.format(DATE_FORMAT)
                                ?: stringResource(R.string.new_trip_end_date_add),
                        )
                    },
                )
                if (uiState.endDate != null) {
                    IconButton(onClick = viewModel::onClearEndDate) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = stringResource(R.string.new_trip_clear_end_date_cd),
                        )
                    }
                }
            }

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
                        PlayerSelectChip(
                            name = player.name,
                            color = PlayerColors.resolve(player.color, player.id.toString()),
                            selected = player.id in uiState.selectedPlayerIds,
                            onClick = { viewModel.onTogglePlayer(player.id) },
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

    if (showEndDatePicker) {
        TripDatePicker(
            initial = uiState.endDate ?: uiState.startDate,
            onPicked = {
                viewModel.onEndDateChange(it)
                showEndDatePicker = false
                notificationPrimer.request(force = false)
            },
            onDismiss = { showEndDatePicker = false },
        )
    }
}

/** Shared by New Trip and Manage trip (same package) — one editable stop in a route. */
@Composable
internal fun StopEditor(
    index: Int,
    total: Int,
    city: String,
    regionId: UUID?,
    regionOptions: List<RegionOption>,
    isError: Boolean,
    onCityChange: (String) -> Unit,
    onRegionSelected: (UUID) -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit,
) {
    val isFirst = index == 0
    val isLast = index == total - 1
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = when {
                    isFirst -> stringResource(R.string.new_trip_stop_start)
                    isLast -> stringResource(R.string.new_trip_stop_destination)
                    else -> stringResource(R.string.new_trip_stop_label, index)
                },
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Row {
                IconButton(onClick = onMoveUp, enabled = !isFirst) {
                    Icon(
                        Icons.Filled.KeyboardArrowUp,
                        contentDescription = stringResource(R.string.new_trip_move_stop_up_cd),
                    )
                }
                IconButton(onClick = onMoveDown, enabled = !isLast) {
                    Icon(
                        Icons.Filled.KeyboardArrowDown,
                        contentDescription = stringResource(R.string.new_trip_move_stop_down_cd),
                    )
                }
                IconButton(onClick = onRemove, enabled = total > 2) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = stringResource(R.string.new_trip_remove_stop_cd),
                    )
                }
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = city,
                onValueChange = onCityChange,
                label = { Text(stringResource(R.string.new_trip_city_label)) },
                singleLine = true,
                isError = isError,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next,
                ),
                modifier = Modifier.weight(1f),
            )
            RegionPickerField(
                label = stringResource(R.string.new_trip_state_label),
                options = regionOptions,
                selectedId = regionId,
                onSelected = onRegionSelected,
                isError = isError,
                modifier = Modifier.width(130.dp),
            )
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
