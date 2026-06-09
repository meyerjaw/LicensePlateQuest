package com.getmecookies.licenseplatequest.ui.screens.passport

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.getmecookies.licenseplatequest.R
import com.getmecookies.licenseplatequest.ui.AppViewModelProvider
import com.getmecookies.licenseplatequest.ui.components.EmptyState
import com.getmecookies.licenseplatequest.ui.components.StateCard
import com.getmecookies.licenseplatequest.ui.map.UsMap
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * The lifetime "Plate Passport": a filled lifetime map, an all-time collected counter, and the
 * list of states caught across every trip with their first-spotted dates. Read-only.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PassportScreen(
    onOpenSettings: () -> Unit = {},
    onOpenState: (String) -> Unit = {},
    viewModel: PassportViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.passport_title)) },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            Icons.Filled.Settings,
                            contentDescription = stringResource(R.string.settings_title),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            when {
                uiState.loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))

                uiState.collectedCount == 0 -> EmptyState(
                    illustrationRes = R.drawable.ic_empty_roadtrip,
                    message = stringResource(R.string.passport_empty),
                    modifier = Modifier.align(Alignment.Center),
                )

                else -> PassportContent(uiState, onOpenState)
            }
        }
    }
}

@Composable
private fun PassportContent(uiState: PassportUiState, onOpenState: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Hero count.
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(
                    R.string.passport_count,
                    uiState.collectedCount,
                    PassportViewModel.TOTAL_STATES,
                ),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = if (uiState.remaining == 0) {
                    stringResource(R.string.passport_complete)
                } else {
                    stringResource(R.string.passport_remaining, uiState.remaining)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // Lifetime filled map (display-only).
        uiState.shapes?.let { shapes ->
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth(),
            ) {
                UsMap(
                    shapes = shapes,
                    foundCodes = uiState.foundCodes,
                    onStateClick = {},
                    interactive = false,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(shapes.width / shapes.height)
                        .padding(12.dp),
                )
            }
        }

        Text(
            text = stringResource(R.string.passport_collected_header),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        uiState.collected.forEach { state ->
            StateCard(
                code = state.code,
                name = state.name,
                found = true,
                subtitle = stringResource(
                    R.string.passport_first_spotted,
                    state.firstFoundAt.atZone(ZoneId.systemDefault()).toLocalDate()
                        .format(DATE_FORMAT),
                    state.firstTripName,
                ),
                badgeLabel = if (state.code in uiState.newToCollection) {
                    stringResource(R.string.passport_new_badge)
                } else {
                    null
                },
                onClick = { onOpenState(state.code) },
            )
        }
    }
}

private val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy")
