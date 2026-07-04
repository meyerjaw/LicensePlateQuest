package com.getmecookies.licenseplatequest.ui.screens.settings

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.getmecookies.licenseplatequest.BuildConfig
import com.getmecookies.licenseplatequest.R
import com.getmecookies.licenseplatequest.data.backup.BackupRepository
import com.getmecookies.licenseplatequest.domain.model.RegionOption
import com.getmecookies.licenseplatequest.domain.model.ThemeMode
import com.getmecookies.licenseplatequest.ui.AppViewModelProvider
import com.getmecookies.licenseplatequest.ui.components.RegionPickerField
import com.getmecookies.licenseplatequest.ui.components.rememberNotificationPermissionPrimer
import java.util.UUID

/** Settings screen (reached from the top-right icon): theme choice and the haptics toggle. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenScan: () -> Unit = {},
    viewModel: SettingsViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val hapticsEnabled by viewModel.hapticsEnabled.collectAsStateWithLifecycle()
    val soundEnabled by viewModel.soundEnabled.collectAsStateWithLifecycle()
    val tripRemindersEnabled by viewModel.tripRemindersEnabled.collectAsStateWithLifecycle()
    val analyticsEnabled by viewModel.analyticsEnabled.collectAsStateWithLifecycle()
    val home by viewModel.home.collectAsStateWithLifecycle()
    val regionOptions by viewModel.regionOptions.collectAsStateWithLifecycle()
    val homeDialog by viewModel.homeDialog.collectAsStateWithLifecycle()

    // Pre-permission primer for the Trip reminders toggle (renders its own dialogs).
    val notificationPrimer = rememberNotificationPermissionPrimer()

    val context = LocalContext.current

    // Debug-only: guard the destructive "wipe all data" action behind a confirm dialog.
    var confirmWipe by remember { mutableStateOf(false) }

    // Backup: import asks replace-vs-merge first (Replace is destructive, so it doubles as confirm).
    var showImportMode by remember { mutableStateOf(false) }
    var pendingImportMode by remember { mutableStateOf<BackupRepository.ImportMode?>(null) }

    // SAF: export writes a new .json the user names/places; import reads one back.
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri -> uri?.let { viewModel.exportTo(it, context.contentResolver) } }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        val mode = pendingImportMode
        if (uri != null && mode != null) viewModel.importFrom(uri, context.contentResolver, mode)
    }

    // Report export/import results via a Toast. Resolve the strings at composition time (the lint
    // forbids LocalContext.current.getString inside the effect).
    val exportOkMsg = stringResource(R.string.settings_backup_export_ok)
    val exportFailedMsg = stringResource(R.string.settings_backup_export_failed)
    val importOkMsg = stringResource(R.string.settings_backup_import_ok)
    val importFailedMsg = stringResource(R.string.settings_backup_import_failed)
    LaunchedEffect(Unit) {
        viewModel.backupEvents.collect { outcome ->
            val msg = when (outcome) {
                BackupOutcome.EXPORT_OK -> exportOkMsg
                BackupOutcome.EXPORT_FAILED -> exportFailedMsg
                BackupOutcome.IMPORT_OK -> importOkMsg
                BackupOutcome.IMPORT_FAILED -> importFailedMsg
            }
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
        }
    }

    // Debug-only: report the seed result (detailed message) via a Toast.
    if (BuildConfig.DEBUG) {
        LaunchedEffect(Unit) {
            viewModel.seedEvents.collect { msg ->
                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
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
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            SettingsSection(title = stringResource(R.string.settings_theme)) {
                ThemeMode.entries.forEach { mode ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.onThemeModeSelected(mode) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = themeMode == mode,
                            onClick = { viewModel.onThemeModeSelected(mode) },
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(text = themeLabel(mode), style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.settings_haptics),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Text(
                        text = stringResource(R.string.settings_haptics_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = hapticsEnabled, onCheckedChange = viewModel::onHapticsToggled)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.settings_sound),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Text(
                        text = stringResource(R.string.settings_sound_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = soundEnabled, onCheckedChange = viewModel::onSoundToggled)
            }

            // Trip reminders (#13): overdue-trip nudge notifications.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.settings_trip_reminders),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Text(
                        text = stringResource(R.string.settings_trip_reminders_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = tripRemindersEnabled,
                    onCheckedChange = { enabled ->
                        if (enabled) {
                            // Turning reminders on is a deliberate action — offer the permission
                            // (force = ignore snooze). Only flip the setting on if it's actually
                            // granted, so declining leaves the toggle off and reminders can fire.
                            notificationPrimer.request(force = true) { granted ->
                                viewModel.onTripRemindersToggled(granted)
                            }
                        } else {
                            viewModel.onTripRemindersToggled(false)
                        }
                    },
                )
            }

            // Anonymous usage analytics (opt-out). Gates all analytics events when off.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.settings_analytics),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Text(
                        text = stringResource(R.string.settings_analytics_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = analyticsEnabled, onCheckedChange = viewModel::onAnalyticsToggled)
            }

            // Home location (#8): pre-fills the New Trip "From" field.
            SettingsSection(title = stringResource(R.string.settings_home)) {
                val homeText = home?.let { h ->
                    val code = regionOptions.firstOrNull { it.id == h.regionId }?.code
                    if (code != null) {
                        stringResource(R.string.settings_home_value, h.city, code)
                    } else {
                        h.city
                    }
                } ?: stringResource(R.string.settings_home_none)
                Column(
                    modifier = Modifier.padding(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(text = homeText, style = MaterialTheme.typography.bodyLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        TextButton(onClick = viewModel::onEditHome) {
                            Text(
                                stringResource(
                                    if (home == null) {
                                        R.string.settings_home_set
                                    } else {
                                        R.string.settings_home_change
                                    },
                                ),
                            )
                        }
                        if (home != null) {
                            TextButton(onClick = viewModel::onClearHome) {
                                Text(stringResource(R.string.settings_home_clear))
                            }
                        }
                    }
                }
            }

            // Backup: export/import all data (basic local backup; online backup is separate).
            SettingsSection(title = stringResource(R.string.settings_backup)) {
                Column(
                    modifier = Modifier.padding(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = stringResource(R.string.settings_backup_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        TextButton(onClick = { exportLauncher.launch(viewModel.suggestedBackupFileName()) }) {
                            Text(stringResource(R.string.settings_backup_export))
                        }
                        TextButton(onClick = { showImportMode = true }) {
                            Text(stringResource(R.string.settings_backup_import))
                        }
                    }
                }
            }

            SettingsSection(title = stringResource(R.string.settings_help)) {
                Column(
                    modifier = Modifier.padding(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = stringResource(R.string.settings_restart_wizard_summary),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    TextButton(onClick = viewModel::restartOnboarding) {
                        Text(stringResource(R.string.settings_restart_wizard))
                    }
                    Text(
                        text = stringResource(R.string.settings_scan_summary),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    TextButton(onClick = onOpenScan) {
                        Text(stringResource(R.string.settings_scan))
                    }
                }
            }

            // Debug-only developer tools (stripped from release builds).
            if (BuildConfig.DEBUG) {
                SettingsSection(title = stringResource(R.string.settings_debug)) {
                    Column(
                        modifier = Modifier.padding(vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.settings_seed_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        TextButton(onClick = viewModel::seedSampleData) {
                            Text(stringResource(R.string.settings_seed_sample))
                        }

                        Text(
                            text = stringResource(R.string.settings_wipe_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        TextButton(onClick = { confirmWipe = true }) {
                            Text(
                                text = stringResource(R.string.settings_wipe),
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                }
            }
        }
    }

    homeDialog?.let { dialog ->
        HomeDialog(
            dialog = dialog,
            regionOptions = regionOptions,
            onCityChange = viewModel::onHomeCityChange,
            onRegionSelected = viewModel::onHomeRegionSelected,
            onSave = viewModel::onHomeDialogSave,
            onDismiss = viewModel::onHomeDialogDismiss,
        )
    }

    if (showImportMode) {
        AlertDialog(
            onDismissRequest = { showImportMode = false },
            title = { Text(stringResource(R.string.settings_backup_import_title)) },
            text = { Text(stringResource(R.string.settings_backup_import_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showImportMode = false
                        pendingImportMode = BackupRepository.ImportMode.REPLACE
                        importLauncher.launch(BACKUP_IMPORT_MIME_TYPES)
                    },
                ) {
                    Text(
                        text = stringResource(R.string.settings_backup_import_replace),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showImportMode = false
                        pendingImportMode = BackupRepository.ImportMode.MERGE
                        importLauncher.launch(BACKUP_IMPORT_MIME_TYPES)
                    },
                ) { Text(stringResource(R.string.settings_backup_import_merge)) }
            },
        )
    }

    if (confirmWipe) {
        AlertDialog(
            onDismissRequest = { confirmWipe = false },
            title = { Text(stringResource(R.string.settings_wipe_confirm_title)) },
            text = { Text(stringResource(R.string.settings_wipe_confirm_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmWipe = false
                        viewModel.wipeAllData()
                    },
                ) {
                    Text(
                        text = stringResource(R.string.settings_wipe_confirm),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmWipe = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}

@Composable
private fun HomeDialog(
    dialog: HomeDialogState,
    regionOptions: List<RegionOption>,
    onCityChange: (String) -> Unit,
    onRegionSelected: (UUID) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_home_dialog_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = dialog.city,
                    onValueChange = onCityChange,
                    label = { Text(stringResource(R.string.settings_home_city)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    modifier = Modifier.fillMaxWidth(),
                )
                RegionPickerField(
                    label = stringResource(R.string.settings_home_state),
                    options = regionOptions,
                    selectedId = dialog.regionId,
                    onSelected = onRegionSelected,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = onSave,
                enabled = dialog.city.isNotBlank() && dialog.regionId != null,
            ) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

@Composable
private fun SettingsSection(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
        )
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                content()
            }
        }
    }
}

/** MIME types the import picker accepts — providers tag exported JSON inconsistently. */
private val BACKUP_IMPORT_MIME_TYPES =
    arrayOf("application/json", "application/octet-stream", "text/plain")

@Composable
private fun themeLabel(mode: ThemeMode): String = stringResource(
    when (mode) {
        ThemeMode.SYSTEM -> R.string.settings_theme_system
        ThemeMode.LIGHT -> R.string.settings_theme_light
        ThemeMode.DARK -> R.string.settings_theme_dark
    },
)
