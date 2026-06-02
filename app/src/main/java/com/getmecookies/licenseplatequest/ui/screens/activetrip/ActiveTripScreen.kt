package com.getmecookies.licenseplatequest.ui.screens.activetrip

import androidx.compose.animation.core.animateIntAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.getmecookies.licenseplatequest.R
import com.getmecookies.licenseplatequest.ui.AppViewModelProvider
import com.getmecookies.licenseplatequest.ui.components.Confetti
import com.getmecookies.licenseplatequest.ui.components.FlagImage
import com.getmecookies.licenseplatequest.ui.map.UsMap
import com.getmecookies.licenseplatequest.ui.screens.celebration.CelebrationMode
import java.util.UUID

/**
 * Active Trip screen (SPEC section 6). A standalone, full-screen view for one trip: a top bar
 * with a Back button (to the Trip List) and an overflow menu (Manage players / End trip), plus
 * two top tabs — **Map** (the interactive US map) and **List** (the found/unfound states list).
 * The chosen tab is remembered across sessions via [ActiveTripViewModel].
 *
 * Celebrations (SPEC section 8): a brief firework burst fires whenever a new state is marked;
 * the 50/50 and manual-end celebrations are launched via [onCelebrate].
 *
 * @param onOpenState open a state's detail (map tap or list row).
 * @param onBack return to the Trip List.
 * @param onCelebrate launch the celebration screen for (tripId, mode).
 * @param onManagePlayers open the manage-players screen for the trip.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveTripScreen(
    onOpenState: (String) -> Unit,
    onBack: () -> Unit,
    onCelebrate: (UUID, CelebrationMode) -> Unit,
    onManagePlayers: (UUID) -> Unit,
    viewModel: ActiveTripViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var menuOpen by remember { mutableStateOf(false) }

    // Fire one-shot celebration navigation (50/50 or manual end).
    LaunchedEffect(uiState.celebration) {
        uiState.celebration?.let {
            onCelebrate(it.tripId, it.mode)
            viewModel.onCelebrationConsumed()
        }
    }

    // Per-state firework + haptic: a local key bumped only by consume-once events from the
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

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                Column {
                    TopAppBar(
                        title = { Text(uiState.tripName.ifBlank { stringResource(R.string.active_trip_title_fallback) }) },
                        navigationIcon = {
                            IconButton(onClick = onBack) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = stringResource(R.string.active_trip_cd_back),
                                )
                            }
                        },
                        actions = {
                            Box {
                                IconButton(onClick = { menuOpen = true }) {
                                    Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.cd_more))
                                }
                                DropdownMenu(
                                    expanded = menuOpen,
                                    onDismissRequest = { menuOpen = false },
                                ) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.active_trip_manage_players)) },
                                        onClick = {
                                            menuOpen = false
                                            uiState.tripId?.let(onManagePlayers)
                                        },
                                    )
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.active_trip_end)) },
                                        onClick = {
                                            menuOpen = false
                                            viewModel.onEndTripClick()
                                        },
                                    )
                                }
                            }
                        },
                    )
                    TabRow(selectedTabIndex = uiState.selectedTab.ordinal) {
                        Tab(
                            selected = uiState.selectedTab == ActiveTripTab.MAP,
                            onClick = { viewModel.onTabSelected(ActiveTripTab.MAP) },
                            text = { Text(stringResource(R.string.active_trip_tab_map)) },
                        )
                        Tab(
                            selected = uiState.selectedTab == ActiveTripTab.LIST,
                            onClick = { viewModel.onTabSelected(ActiveTripTab.LIST) },
                            text = { Text(stringResource(R.string.active_trip_tab_list)) },
                        )
                    }
                }
            },
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                when (uiState.selectedTab) {
                    ActiveTripTab.MAP -> {
                        val shapes = uiState.shapes
                        if (shapes == null) {
                            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                        } else {
                            UsMap(
                                shapes = shapes,
                                foundCodes = uiState.foundCodes,
                                onStateClick = onOpenState,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(8.dp),
                            )
                        }
                    }
                    ActiveTripTab.LIST -> {
                        if (uiState.loading) {
                            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                        } else {
                            FoundStatesList(
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
                        }
                    }
                }
            }
        }

        // Brief firework burst on each new find. Keyed on a consume-once counter so it fires
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
            title = { Text(stringResource(R.string.active_trip_end_title)) },
            text = { Text(stringResource(R.string.active_trip_end_body, uiState.tripName)) },
            confirmButton = { TextButton(onClick = viewModel::onConfirmEndTrip) { Text(stringResource(R.string.active_trip_end)) } },
            dismissButton = { TextButton(onClick = viewModel::onDismissEndDialog) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}

/** The found-states list (its own tab). Header (count + sort), a search box, a show-unfound
 *  toggle, then the scrolling list of state rows. */
@Composable
private fun FoundStatesList(
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
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .padding(top = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val animatedCount by animateIntAsState(targetValue = count, label = "foundCount")
            Text(
                text = stringResource(R.string.active_trip_found_count, animatedCount),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = sort == FoundSort.ORDER_FOUND,
                    onClick = { onSortChange(FoundSort.ORDER_FOUND) },
                    label = { Text(stringResource(R.string.active_trip_sort_order_found)) },
                )
                FilterChip(
                    selected = sort == FoundSort.ALPHABETICAL,
                    onClick = { onSortChange(FoundSort.ALPHABETICAL) },
                    label = { Text(stringResource(R.string.active_trip_sort_alphabetical)) },
                )
            }
        }

        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(stringResource(R.string.active_trip_search_placeholder)) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchChange("") }) {
                        Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.active_trip_cd_clear_search))
                    }
                }
            },
            singleLine = true,
        )

        FilterChip(
            selected = showUnfound,
            onClick = { onToggleShowUnfound(!showUnfound) },
            label = { Text(stringResource(R.string.active_trip_show_unfound)) },
            leadingIcon = if (showUnfound) {
                { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
            } else {
                null
            },
        )

        if (states.isEmpty()) {
            val message = when {
                searchQuery.isNotBlank() -> stringResource(R.string.active_trip_no_match, searchQuery)
                else -> stringResource(R.string.active_trip_no_states)
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
                    .weight(1f),
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
                    text = stringResource(R.string.active_trip_not_found_yet),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
