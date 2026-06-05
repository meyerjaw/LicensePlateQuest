package com.getmecookies.licenseplatequest.ui.screens.trips

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.getmecookies.licenseplatequest.R
import com.getmecookies.licenseplatequest.domain.model.RegionOption
import com.getmecookies.licenseplatequest.ui.AppViewModelProvider
import com.getmecookies.licenseplatequest.ui.PlayerColors
import com.getmecookies.licenseplatequest.ui.components.PlayerSelectChip
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID

/**
 * Full-screen Manage trip (edit) form (playtest #14). Same shape as the New Trip form but
 * prefilled from an existing trip and committed on "Save changes". All edits are staged; backing
 * out with unsaved changes prompts before discarding. Players are edited inline (one section)
 * rather than on a separate screen.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ManageTripScreen(
    onDone: () -> Unit,
    onAddPlayer: () -> Unit,
    addedPlayerId: String? = null,
    onAddedPlayerConsumed: () -> Unit = {},
    viewModel: ManageTripViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val saved by viewModel.saved.collectAsStateWithLifecycle()

    LaunchedEffect(saved) {
        if (saved) onDone()
    }

    LaunchedEffect(addedPlayerId) {
        if (addedPlayerId != null) {
            viewModel.onExternalPlayerAdded(UUID.fromString(addedPlayerId))
            onAddedPlayerConsumed()
        }
    }

    var showDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    var showDiscardDialog by remember { mutableStateOf(false) }

    // Changing the end date can schedule an overdue reminder, so ask for notification permission
    // the first time one is set here (Android 13+). Granting is optional — saving isn't blocked.
    val context = LocalContext.current
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* outcome doesn't block the edit; the worker re-checks permission before posting. */ }
    fun maybeRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // Intercept back: warn before throwing away unsaved edits.
    fun attemptExit() {
        if (viewModel.isDirty()) showDiscardDialog = true else onDone()
    }
    BackHandler(enabled = !uiState.loading) { attemptExit() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.manage_trip_title)) },
                navigationIcon = {
                    IconButton(onClick = { attemptExit() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        if (uiState.loading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
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
                RegionDropdown(
                    label = stringResource(R.string.new_trip_state_label),
                    options = uiState.regionOptions,
                    selected = uiState.originRegion,
                    isError = uiState.showErrors && !uiState.originRegionValid,
                    onSelected = viewModel::onOriginRegionSelected,
                    modifier = Modifier.width(130.dp),
                )
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
                RegionDropdown(
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

            // --- Trip name ----------------------------------------------
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
                label = { Text(uiState.startDate.format(MANAGE_TRIP_DATE_FORMAT)) },
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
                            uiState.endDate?.format(MANAGE_TRIP_DATE_FORMAT)
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
                Text(stringResource(R.string.manage_trip_save))
            }
        }
    }

    if (showDatePicker) {
        ManageTripDatePicker(
            initial = uiState.startDate,
            onPicked = {
                viewModel.onStartDateChange(it)
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false },
        )
    }

    if (showEndDatePicker) {
        ManageTripDatePicker(
            initial = uiState.endDate ?: uiState.startDate,
            onPicked = {
                viewModel.onEndDateChange(it)
                showEndDatePicker = false
                maybeRequestNotificationPermission()
            },
            onDismiss = { showEndDatePicker = false },
        )
    }

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text(stringResource(R.string.manage_trip_discard_title)) },
            text = { Text(stringResource(R.string.manage_trip_discard_body)) },
            confirmButton = {
                TextButton(onClick = {
                    showDiscardDialog = false
                    onDone()
                }) { Text(stringResource(R.string.manage_trip_discard_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) {
                    Text(stringResource(R.string.manage_trip_keep_editing))
                }
            },
        )
    }
}

@Composable
private fun RegionDropdown(
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
private fun ManageTripDatePicker(
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

private val MANAGE_TRIP_DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy")
