package com.getmecookies.licenseplatequest.ui.screens.statedetail

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.getmecookies.licenseplatequest.R
import com.getmecookies.licenseplatequest.domain.model.Player
import com.getmecookies.licenseplatequest.domain.model.StateDetailData
import com.getmecookies.licenseplatequest.ui.AppViewModelProvider
import com.getmecookies.licenseplatequest.ui.PlayerColors
import com.getmecookies.licenseplatequest.ui.components.FlagImage
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID

/**
 * State Detail (SPEC section 6). Shows the state's bundled facts and plate image. When the
 * active trip hasn't found it, offers "Mark as found" (commits immediately, then returns to
 * the map); when found, shows the found timestamp and trip and offers "Unmark" (confirmed).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StateDetailScreen(
    onBack: () -> Unit,
    viewModel: StateDetailViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val data = uiState.data

    // Marking commits immediately and returns to the map.
    LaunchedEffect(uiState.markComplete) {
        if (uiState.markComplete) onBack()
    }

    // Leaving with unsaved attribution edits warns first; otherwise just goes back.
    val onBackPressed: () -> Unit = {
        if (uiState.hasUnsavedAttribution) viewModel.onConfirmDiscardChanges() else onBack()
    }
    BackHandler(enabled = uiState.hasUnsavedAttribution) {
        viewModel.onConfirmDiscardChanges()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(data?.info?.name ?: stringResource(R.string.state_detail_title_fallback)) },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back),
                        )
                    }
                },
                actions = {
                    // Save (✓) edited attribution on a found state (playtest note #17).
                    if (uiState.hasUnsavedAttribution) {
                        IconButton(onClick = viewModel::onSaveAttribution) {
                            Icon(
                                Icons.Filled.Check,
                                contentDescription = stringResource(R.string.state_detail_save_attribution),
                            )
                        }
                    }
                },
            )
        },
        // The primary action is pinned to the bottom so it's always reachable without scrolling.
        bottomBar = {
            if (data != null) {
                StateDetailActionBar(
                    data = data,
                    onMark = viewModel::onMarkClick,
                    onUnmark = viewModel::onUnmarkClick,
                )
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center,
        ) {
            when {
                uiState.loading -> CircularProgressIndicator()
                data == null -> Text(stringResource(R.string.state_detail_not_found))
                else -> StateDetailContent(
                    data = data,
                    selectedPlayerIds = uiState.selectedPlayerIds,
                    onTogglePlayer = viewModel::onTogglePlayer,
                )
            }
        }
    }

    when (uiState.dialog) {
        StateDetailDialog.NONE -> Unit
        StateDetailDialog.CONFIRM_UNMARK -> ConfirmDialog(
            title = stringResource(R.string.state_detail_unmark_title),
            body = stringResource(
                R.string.state_detail_unmark_body,
                data?.info?.name ?: stringResource(R.string.state_detail_this_state),
            ),
            confirmLabel = stringResource(R.string.state_detail_unmark),
            onConfirm = viewModel::onConfirmUnmark,
            onDismiss = viewModel::onDismissDialog,
        )
        StateDetailDialog.CONFIRM_DISCARD -> ConfirmDialog(
            title = stringResource(R.string.state_detail_discard_title),
            body = stringResource(R.string.state_detail_discard_body),
            confirmLabel = stringResource(R.string.state_detail_discard_confirm),
            onConfirm = onBack,
            onDismiss = viewModel::onDismissDialog,
        )
    }
}

@Composable
private fun StateDetailContent(
    data: StateDetailData,
    selectedPlayerIds: Set<UUID>,
    onTogglePlayer: (UUID) -> Unit,
) {
    val info = data.info
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Flag, framed so it reads as a single hero element regardless of flag shape.
        Surface(
            shape = RoundedCornerShape(12.dp),
            tonalElevation = 1.dp,
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth(),
        ) {
            FlagImage(
                code = info.code,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
            )
        }

        // When-found banner sits up top once a state is in the trip.
        if (data.found) {
            FoundBanner(data)
        }

        // Attribution multi-select, only meaningful with 2+ players on the trip (note #17).
        if (data.hasActiveTrip && data.tripPlayers.size >= 2) {
            AttributionCard(
                players = data.tripPlayers,
                selectedIds = selectedPlayerIds,
                found = data.found,
                onToggle = onTogglePlayer,
            )
        }

        SectionCard(title = stringResource(R.string.state_detail_symbols)) {
            FactRow(label = stringResource(R.string.state_detail_bird), value = info.bird)
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            FactRow(label = stringResource(R.string.state_detail_flower), value = info.flower)
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            FactRow(label = stringResource(R.string.state_detail_motto), value = info.motto)
        }

        if (info.funFacts.isNotEmpty()) {
            SectionCard(title = stringResource(R.string.state_detail_fun_facts)) {
                info.funFacts.forEachIndexed { index, fact ->
                    if (index > 0) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(R.string.state_detail_bullet), style = MaterialTheme.typography.bodyMedium)
                        Text(fact, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

/** Player multi-select for crediting a find (playtest note #17). Colored chips toggle credit;
 *  for an already-found state, each toggle persists immediately. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AttributionCard(
    players: List<Player>,
    selectedIds: Set<UUID>,
    found: Boolean,
    onToggle: (UUID) -> Unit,
) {
    val title = stringResource(
        if (found) R.string.state_detail_spotted_by else R.string.state_detail_who_spotted,
    )
    SectionCard(title = title) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            players.forEach { player ->
                FilterChip(
                    selected = player.id in selectedIds,
                    onClick = { onToggle(player.id) },
                    label = { Text(player.name) },
                    leadingIcon = {
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(
                                    PlayerColors.resolve(player.color, player.id.toString()),
                                ),
                        )
                    },
                )
            }
        }
    }
}

/** The primary mark/unmark action, pinned to the bottom of the screen above the system bar. */
@Composable
private fun StateDetailActionBar(
    data: StateDetailData,
    onMark: () -> Unit,
    onUnmark: () -> Unit,
) {
    Surface(
        tonalElevation = 3.dp,
        shadowElevation = 8.dp,
        color = MaterialTheme.colorScheme.surface,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            when {
                data.found -> OutlinedButton(
                    onClick = onUnmark,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.state_detail_unmark))
                }
                data.hasActiveTrip -> Button(
                    onClick = onMark,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.state_detail_mark))
                }
                else -> Text(
                    text = stringResource(R.string.state_detail_no_trip),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

/** A titled card section with a subtle background, used to group related rows. */
@Composable
private fun SectionCard(
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
private fun FactRow(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(text = value, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun FoundBanner(data: StateDetailData) {
    val whenText = data.foundAt?.let {
        DATE_FORMAT.format(it.atZone(ZoneId.systemDefault()))
    }
    val parts = buildList {
        if (whenText != null) add(stringResource(R.string.state_detail_found_when, whenText))
        data.foundTripName?.let { add(stringResource(R.string.state_detail_found_on, it)) }
    }
    if (parts.isNotEmpty()) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = parts.joinToString(" "),
                style = MaterialTheme.typography.bodyMedium,
                fontStyle = FontStyle.Italic,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            )
        }
    }
}

@Composable
private fun ConfirmDialog(
    title: String,
    body: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(body) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(confirmLabel) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

private val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a")
