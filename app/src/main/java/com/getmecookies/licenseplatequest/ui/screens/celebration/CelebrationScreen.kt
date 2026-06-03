package com.getmecookies.licenseplatequest.ui.screens.celebration

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.getmecookies.licenseplatequest.R
import com.getmecookies.licenseplatequest.domain.model.CelebrationStats
import com.getmecookies.licenseplatequest.ui.AppViewModelProvider
import com.getmecookies.licenseplatequest.ui.components.Confetti
import com.getmecookies.licenseplatequest.ui.map.UsMap

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
    val isSummary = uiState.mode == CelebrationMode.SUMMARY
    val stats = uiState.stats

    Box(modifier = Modifier.fillMaxSize()) {
        if (uiState.loading || stats == null) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Headline hero block.
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 20.dp, horizontal = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = when {
                                isFifty -> stringResource(R.string.celebration_all_fifty)
                                isSummary -> stringResource(R.string.celebration_summary)
                                else -> stringResource(R.string.celebration_made_it_home)
                            },
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                        )
                        Text(
                            text = if (isFifty) {
                                stringResource(R.string.celebration_congrats, stats.tripName)
                            } else {
                                stats.tripName
                            },
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Center,
                        )
                    }
                }

                // The colorful filled-in map as the summary hero (playtest note #3). Shown even
                // at 0 found (blank) — it's part of the summary's identity. Non-interactive so it
                // sits inside the scrolling column without capturing the scroll.
                uiState.mapShapes?.let { shapes ->
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

                StatsSections(stats)

                Button(
                    onClick = {
                        // Summary + 50/50 just dismiss; only an active manual-end finalizes.
                        if (isFifty || isSummary) onExit() else viewModel.onFinishManualEnd()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                ) {
                    Text(
                        if (isFifty) {
                            stringResource(R.string.celebration_continue)
                        } else {
                            stringResource(R.string.celebration_done)
                        },
                    )
                }
            }
        }

        // Confetti only celebrates a fresh achievement; a re-opened summary is calm.
        if (!isSummary) {
            Confetti(
                trigger = "celebration",
                particleCount = if (isFifty) 160 else 90,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

/**
 * A single label/value pair, or null if the value is absent (so empty rows are skipped).
 * [info], when set, adds a tappable info icon that opens an explanatory dialog.
 */
private data class Stat(val label: String, val value: String?, val info: String? = null)

@Composable
private fun StatsSections(stats: CelebrationStats) {
    val progress = listOf(
        Stat(
            stringResource(R.string.celebration_stat_states_found),
            stringResource(R.string.celebration_states_found, stats.foundCount),
        ),
        Stat(stringResource(R.string.celebration_stat_trip_duration), stats.durationText),
        Stat(
            label = stringResource(R.string.celebration_stat_estimated_distance),
            value = stats.estimatedDistanceText,
            info = stringResource(R.string.celebration_estimated_distance_info),
        ),
    )
    val timing = listOf(
        Stat(stringResource(R.string.celebration_stat_average_gap), stats.averageGapText),
        Stat(stringResource(R.string.celebration_stat_longest_gap), stats.longestGapText),
        Stat(stringResource(R.string.celebration_stat_shortest_gap), stats.shortestGapText),
    )
    val highlights = listOf(
        Stat(stringResource(R.string.celebration_stat_first_state), stats.firstStateName),
        Stat(stringResource(R.string.celebration_stat_last_state), stats.lastStateName),
        Stat(stringResource(R.string.celebration_stat_furthest_state), stats.furthestStateName),
        Stat(stringResource(R.string.celebration_stat_rarest_state), stats.rarestStateName),
    )

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        StatSection(stringResource(R.string.celebration_section_progress), progress)
        StatSection(stringResource(R.string.celebration_section_timing), timing)
        StatSection(stringResource(R.string.celebration_section_highlights), highlights)
        if (stats.playerNames.isNotEmpty()) {
            StatSection(
                stringResource(R.string.celebration_section_players),
                listOf(
                    Stat(
                        stringResource(R.string.celebration_on_this_trip),
                        stats.playerNames.joinToString(", "),
                    ),
                ),
            )
        }
    }
}

/** A titled card grouping related stats; rows with no value are dropped entirely. */
@Composable
private fun StatSection(title: String, stats: List<Stat>) {
    val rows = stats.filter { it.value != null }
    if (rows.isEmpty()) return

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
        )
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                rows.forEachIndexed { index, stat ->
                    if (index > 0) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                    StatRow(stat)
                }
            }
        }
    }
}

@Composable
private fun StatRow(stat: Stat) {
    var showInfo by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = stat.label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (stat.info != null) {
                IconButton(
                    onClick = { showInfo = true },
                    modifier = Modifier.size(20.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = stringResource(R.string.celebration_cd_about_stat, stat.label),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
        Text(
            text = stat.value.orEmpty(),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }

    if (showInfo && stat.info != null) {
        AlertDialog(
            onDismissRequest = { showInfo = false },
            title = { Text(stat.label) },
            text = { Text(stat.info) },
            confirmButton = { TextButton(onClick = { showInfo = false }) { Text(stringResource(R.string.action_got_it)) } },
        )
    }
}
