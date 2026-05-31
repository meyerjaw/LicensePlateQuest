package com.getmecookies.licenseplatequest.ui.screens.activetrip

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.MoreVert
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.getmecookies.licenseplatequest.domain.model.FoundState
import com.getmecookies.licenseplatequest.ui.AppViewModelProvider
import com.getmecookies.licenseplatequest.ui.components.PlateImage
import com.getmecookies.licenseplatequest.ui.map.UsMap
import androidx.compose.ui.unit.sp

/**
 * Active Trip View (SPEC section 6). The home screen while a trip is active: the trip name up
 * top, the interactive US map (found states filled), a persistent X/50 counter, and a
 * collapsible bottom sheet listing found states — sortable by order found or alphabetically,
 * each row tappable to open State Detail. An overflow menu ends the trip (with confirmation).
 *
 * Tapping a state on the map or a row in the sheet opens that state's detail via [onOpenState].
 * [onViewAllTrips] returns to the full Trip List.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveTripScreen(
    onOpenState: (String) -> Unit,
    onViewAllTrips: () -> Unit,
    viewModel: ActiveTripViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scaffoldState = rememberBottomSheetScaffoldState()
    var menuOpen by remember { mutableStateOf(false) }

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetPeekHeight = 96.dp,
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
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
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
                foundStates = uiState.foundStates,
                onSortChange = viewModel::onSortChange,
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
    foundStates: List<FoundState>,
    onSortChange: (FoundSort) -> Unit,
    onRowClick: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "$count / 50 found",
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

        if (foundStates.isEmpty()) {
            Text(
                text = "No states yet — tap a state on the map when you spot its plate.",
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
                items(foundStates, key = { it.code }) { state ->
                    FoundStateRowItem(state = state, onClick = { onRowClick(state.code) })
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun FoundStateRowItem(state: FoundState, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        PlateImage(
            code = state.code,
            assetPath = state.plateImagePath,
            modifier = Modifier.width(64.dp),
            placeholderFontSize = 16.sp,
        )
        Text(text = state.name, style = MaterialTheme.typography.bodyLarge)
    }
}
