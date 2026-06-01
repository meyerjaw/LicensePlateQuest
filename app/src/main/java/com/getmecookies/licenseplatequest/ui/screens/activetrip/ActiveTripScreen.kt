package com.getmecookies.licenseplatequest.ui.screens.activetrip

import androidx.compose.animation.core.animateIntAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.getmecookies.licenseplatequest.ui.AppViewModelProvider
import com.getmecookies.licenseplatequest.ui.components.Confetti
import com.getmecookies.licenseplatequest.ui.components.FlagImage
import com.getmecookies.licenseplatequest.ui.map.UsMap
import com.getmecookies.licenseplatequest.ui.screens.celebration.CelebrationMode
import java.util.UUID

/**
 * Active Trip View (SPEC section 6). The home screen while a trip is active: the trip name up
 * top, the interactive US map (found states filled), a persistent X/50 counter, and a
 * collapsible bottom sheet listing found states — sortable by order found or alphabetically,
 * each row tappable to open State Detail. An overflow menu ends the trip (with confirmation).
 *
 * Celebrations (SPEC section 8): a brief confetti burst fires whenever a new state is marked;
 * the 50/50 and manual-end celebrations are launched via [onCelebrate].
 *
 * @param onOpenState open a state's detail (map tap or sheet row).
 * @param onViewAllTrips return to the full Trip List.
 * @param onCelebrate launch the celebration screen for (tripId, mode).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveTripScreen(
    onOpenState: (String) -> Unit,
    onViewAllTrips: () -> Unit,
    onCelebrate: (UUID, CelebrationMode) -> Unit,
    viewModel: ActiveTripViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scaffoldState = rememberBottomSheetScaffoldState()
    var menuOpen by remember { mutableStateOf(false) }

    // Fire one-shot celebration navigation (50/50 or manual end).
    LaunchedEffect(uiState.celebration) {
        uiState.celebration?.let {
            onCelebrate(it.tripId, it.mode)
            viewModel.onCelebrationConsumed()
        }
    }

    // Per-state confetti + haptic: a local key bumped only by consume-once events from the
    // ViewModel, so feedback fires exactly once per new find and never replays on returning
    // from State Detail.
    val haptics = LocalHapticFeedback.current
    var confettiKey by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        viewModel.confettiEvents.collect {
            confettiKey++
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    // The system navigation-bar inset (this screen is full-screen, so the sheet must clear it
    // itself). Used both to lift the collapsed handle above the bar and to pad the expanded list.
    val navBarInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Box(modifier = Modifier.fillMaxSize()) {
        BottomSheetScaffold(
            scaffoldState = scaffoldState,
            // A custom handle with a known height; the peek matches it exactly so the collapsed
            // sheet shows ONLY the pull-up bar. The handle carries the nav-bar inset below it so
            // the bar sits above the system navigation bar; none of the list content peeks.
            sheetPeekHeight = SHEET_HANDLE_HEIGHT + navBarInset,
            sheetDragHandle = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp, bottom = 12.dp + navBarInset),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 32.dp, height = 4.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)),
                    )
                }
            },
            topBar = {
                TopAppBar(
                    title = { Text(uiState.tripName.ifBlank { "Active trip" }) },
                    navigationIcon = {
                        IconButton(onClick = onViewAllTrips) {
                            Icon(Icons.AutoMirrored.Filled.List, contentDescription = "All trips")
                        }
                    },
                    actions = {
                        Box {
                            IconButton(onClick = { menuOpen = true }) {
                                Icon(Icons.Filled.MoreVert, contentDescription = "More")
                            }
                            DropdownMenu(
                                expanded = menuOpen,
                                onDismissRequest = { menuOpen = false },
                            ) {
                                DropdownMenuItem(
                                    text = { Text("End trip") },
                                    onClick = {
                                        menuOpen = false
                                        viewModel.onEndTripClick()
                                    },
                                )
                            }
                        }
                    },
                )
            },
            sheetContent = {
                FoundStatesSheet(
                    count = uiState.foundCount,
                    sort = uiState.sort,
                    states = uiState.states,
                    searchQuery = uiState.searchQuery,
                    showUnfound = uiState.showUnfound,
                    onSortChange = viewModel::onSortChange,
                    onSearchChange = viewModel::onSearchChange,
                    onToggleShowUnfound = viewModel::onToggleShowUnfound,
                    onRowClick = onOpenState,
                )
            },
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                val shapes = uiState.shapes
                when {
                    uiState.loading || shapes == null -> CircularProgressIndicator()
                    else -> UsMap(
                        shapes = shapes,
                        foundCodes = uiState.foundCodes,
                        onStateClick = onOpenState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp),
                    )
                }
            }
        }

        // Brief confetti burst on each new find. Keyed on a consume-once counter so it fires
        // only on an actual mark, never on returning to this screen.
        if (confettiKey > 0) {
            Confetti(
                trigger = confettiKey,
                particleCount = 150,
                durationMillis = 1500,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }

    if (uiState.showEndDialog) {
        AlertDialog(
            onDismissRequest = viewModel::onDismissEndDialog,
            title = { Text("End this trip?") },
            text = { Text("End \"${uiState.tripName}\"? You can still view it afterward, but it'll move to your completed trips.") },
            confirmButton = { TextButton(onClick = viewModel::onConfirmEndTrip) { Text("End trip") } },
            dismissButton = { TextButton(onClick = viewModel::onDismissEndDialog) { Text("Cancel") } },
        )
    }
}

@Composable
private fun FoundStatesSheet(
    count: Int,
    sort: FoundSort,
    states: List<StateRow>,
    searchQuery: String,
    showUnfound: Boolean,
    onSortChange: (FoundSort) -> Unit,
    onSearchChange: (String) -> Unit,
    onToggleShowUnfound: (Boolean) -> Unit,
    onRowClick: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val animatedCount by animateIntAsState(targetValue = count, label = "foundCount")
            Text(
                text = "$animatedCount / 50 found",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = sort == FoundSort.ORDER_FOUND,
                    onClick = { onSortChange(FoundSort.ORDER_FOUND) },
                    label = { Text("Order found") },
                )
                FilterChip(
                    selected = sort == FoundSort.ALPHABETICAL,
                    onClick = { onSortChange(FoundSort.ALPHABETICAL) },
                    label = { Text("A–Z") },
                )
            }
        }

        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search states") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchChange("") }) {
                        Icon(Icons.Filled.Close, contentDescription = "Clear search")
                    }
                }
            },
            singleLine = true,
        )

        FilterChip(
            selected = showUnfound,
            onClick = { onToggleShowUnfound(!showUnfound) },
            label = { Text("Show unfound states") },
            leadingIcon = if (showUnfound) {
                { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
            } else {
                null
            },
        )

        if (states.isEmpty()) {
            val message = when {
                searchQuery.isNotBlank() -> "No states match “$searchQuery”."
                else -> "No states yet — tap a state on the map when you spot its plate."
            }
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 16.dp),
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items(states, key = { it.code }) { state ->
                    StateRowItem(state = state, onClick = { onRowClick(state.code) })
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun StateRowItem(state: StateRow, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        FlagImage(
            code = state.code,
            modifier = Modifier
                .width(64.dp)
                .alpha(if (state.found) 1f else 0.4f),
            placeholderFontSize = 16.sp,
        )
        Column {
            Text(
                text = state.name,
                style = MaterialTheme.typography.bodyLarge,
                color = if (state.found) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
            if (!state.found) {
                Text(
                    text = "Not found yet",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** Height of the custom drag handle (12dp top pad + 4dp bar + 12dp bottom pad); the collapsed
 *  sheet peek matches this so only the pull-up bar shows. */
private val SHEET_HANDLE_HEIGHT = 28.dp
