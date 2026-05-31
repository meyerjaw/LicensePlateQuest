package com.getmecookies.licenseplatequest.ui.screens.celebration

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.getmecookies.licenseplatequest.domain.model.CelebrationStats
import com.getmecookies.licenseplatequest.ui.AppViewModelProvider
import com.getmecookies.licenseplatequest.ui.components.Confetti

/**
 * Celebration screen (SPEC section 6), shared by the 50/50 and manual-end variants. Shows a
 * confetti burst, a headline, and the trip stats. The 50/50 variant's "Continue" returns to
 * the active trip (trip stays active); the manual-end variant's "Done" finalizes the trip and
 * returns to the trip list.
 *
 * @param onExit dismiss this screen (pop). For manual end, called after the trip is finalized.
 */
@Composable
fun CelebrationScreen(
    onExit: () -> Unit,
    viewModel: CelebrationViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // After a manual end is finalized, leave to the trip list.
    LaunchedEffect(uiState.finished) {
        if (uiState.finished) onExit()
    }

    val isFifty = uiState.mode == CelebrationMode.FIFTY_FIFTY
    val stats = uiState.stats

    Box(modifier = Modifier.fillMaxSize()) {
        if (uiState.loading || stats == null) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = if (isFifty) "All 50!" else "Made it home!",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = if (isFifty) "Congratulations, ${stats.tripName}!" else stats.tripName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                )

                StatsCard(stats)

                Button(
                    onClick = {
                        if (isFifty) onExit() else viewModel.onFinishManualEnd()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                ) {
                    Text(if (isFifty) "Continue" else "Done")
                }
            }
        }

        // Confetti overlay on top of everything; smaller burst for the manual-end variant.
        Confetti(
            trigger = "celebration",
            particleCount = if (isFifty) 160 else 90,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun StatsCard(stats: CelebrationStats) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        StatRow("States found", "${stats.foundCount} / 50")
        StatRow("Trip duration", stats.durationText)
        stats.averageGapText?.let { StatRow("Average time between finds", it) }
        stats.longestGapText?.let { StatRow("Longest gap", it) }
        stats.shortestGapText?.let { StatRow("Quickest back-to-back", it) }
        stats.firstStateName?.let { StatRow("First state", it) }
        stats.lastStateName?.let { StatRow("Last state", it) }
        stats.estimatedDistanceText?.let { StatRow("Estimated distance", it) }
        stats.furthestStateName?.let { StatRow("Furthest from home", it) }
        stats.rarestStateName?.let { StatRow("Rarest find", it) }
        if (stats.playerNames.isNotEmpty()) {
            StatRow("Players", stats.playerNames.joinToString(", "))
        }
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
