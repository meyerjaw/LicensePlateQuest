package com.getmecookies.licenseplatequest.ui.screens.activetrip

import androidx.compose.animation.core.animateIntAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Map
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
import androidx.compose.material3.Surface
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
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
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
import java.time.Instant
import java.time.temporal.ChronoUnit
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
 * @param onManageTrip open the Manage trip (edit) screen for the trip.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveTripScreen(
    onOpenState: (String) -> Unit,
    onBack: () -> Unit,
    onCelebrate: (UUID, CelebrationMode) -> Unit,
    onManageTrip: (UUID) -> Unit,
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
            if (viewModel.hapticsEnabled.value) {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            }
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
                                        text = { Text(stringResource(R.string.active_trip_manage_trip)) },
                                        onClick = {
                                            menuOpen = false
                                            uiState.tripId?.let(onManageTrip)
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
                        val mapSelected = uiState.selectedTab == ActiveTripTab.MAP
                        val listSelected = uiState.selectedTab == ActiveTripTab.LIST
                        Tab(
                            selected = mapSelected,
                            onClick = { viewModel.onTabSelected(ActiveTripTab.MAP) },
                            text = { Text(stringResource(R.string.active_trip_tab_map)) },
                            // The label and the TabRow's selected indicator already cue the active
                            // tab; the icon is a recognizability aid (playtest note #23).
                            icon = { Icon(Icons.Filled.Map, contentDescription = null) },
                        )
                        Tab(
                            selected = listSelected,
                            onClick = { viewModel.onTabSelected(ActiveTripTab.LIST) },
                            text = { Text(stringResource(R.string.active_trip_tab_list)) },
                            icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = null) },
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
                            Column(modifier = Modifier.fillMaxSize()) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth(),
                                ) {
                                    UsMap(
                                        shapes = shapes,
                                        foundCodes = uiState.foundCodes,
                                        onStateClick = onOpenState,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(8.dp),
                                    )
                                    // At-a-glance progress while playing on the map (note #2).
                                    MapStateCounter(
                                        count = uiState.foundCount,
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(16.dp),
                                    )
                                }
                                // Tight stats strip beneath the map (playtest note #21).
                                MapStatsStrip(
                                    stats = uiState.mapStats,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
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

/** A small "X / 50" progress pill overlaid on the Map tab. The number animates on change and
 *  the whole pill reads as one accessible announcement (playtest note #2). */
@Composable
private fun MapStateCounter(count: Int, modifier: Modifier = Modifier) {
    val animatedCount by animateIntAsState(targetValue = count, label = "mapFoundCount")
    val description = stringResource(R.string.active_trip_map_counter_cd, animatedCount)
    Surface(
        modifier = modifier.clearAndSetSemantics { contentDescription = description },
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        shadowElevation = 3.dp,
    ) {
        Text(
            text = stringResource(R.string.active_trip_map_counter, animatedCount),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
        )
    }
}

/** A tight, horizontally-scrolling row of at-a-glance stat cards beneath the map (note #21). */
@Composable
private fun MapStatsStrip(stats: MapStats, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        StatCard(
            value = stringResource(R.string.map_stat_found_value, stats.foundCount),
            label = stringResource(R.string.map_stat_found_label),
        )
        StatCard(
            value = "${stats.percent}%",
            label = stringResource(R.string.map_stat_complete_label),
        )
        if (stats.lastFoundName != null) {
            StatCard(
                value = stats.lastFoundName,
                label = stringResource(R.string.map_stat_last_label, relativeAgo(stats.lastFoundAt)),
            )
        }
        StatCard(
            value = stringResource(R.string.map_stat_day_value, stats.dayOfTrip),
            label = stringResource(R.string.map_stat_day_label),
        )
        StatCard(
            value = stats.foundToday.toString(),
            label = stringResource(R.string.map_stat_today_label),
        )
    }
}

@Composable
private fun StatCard(value: String, label: String) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column(
            modifier = Modifier
                .widthIn(min = 72.dp)
                .padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
    }
}

/** "just now" / "12m ago" / "3h ago" / "2d ago" for the last-find card. */
@Composable
private fun relativeAgo(instant: Instant?): String {
    if (instant == null) return stringResource(R.string.map_time_now)
    val mins = ChronoUnit.MINUTES.between(instant, Instant.now())
    return when {
        mins < 1L -> stringResource(R.string.map_time_now)
        mins < 60L -> stringResource(R.string.map_time_minutes, mins.toInt())
        mins < 1440L -> stringResource(R.string.map_time_hours, (mins / 60).toInt())
        else -> stringResource(R.string.map_time_days, (mins / 1440).toInt())
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
