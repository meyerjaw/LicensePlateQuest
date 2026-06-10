package com.getmecookies.licenseplatequest.ui.screens.celebration

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Share
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.getmecookies.licenseplatequest.R
import com.getmecookies.licenseplatequest.domain.model.CelebrationStats
import com.getmecookies.licenseplatequest.domain.model.PlayerScore
import com.getmecookies.licenseplatequest.domain.model.TimelineFind
import com.getmecookies.licenseplatequest.ui.AppViewModelProvider
import com.getmecookies.licenseplatequest.ui.components.FlagImage
import com.getmecookies.licenseplatequest.ui.PlayerColors
import com.getmecookies.licenseplatequest.ui.components.Confetti
import com.getmecookies.licenseplatequest.ui.map.UsMap
import com.getmecookies.licenseplatequest.ui.map.UsMapShapes
import com.getmecookies.licenseplatequest.ui.share.shareTripImage
import java.time.LocalDate
import java.time.format.DateTimeFormatter

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

    val context = LocalContext.current
    val shareChooserTitle = stringResource(R.string.share_chooser_title)
    val graphicsLayer = rememberGraphicsLayer()
    var shareRequested by remember { mutableStateOf(false) }
    // The launcher icon, rendered to a bitmap for the share watermark. Via the package manager so
    // the adaptive icon is flattened correctly (painterResource can't render adaptive icons).
    val appIcon: ImageBitmap? = remember {
        runCatching {
            context.packageManager.getApplicationIcon(context.packageName)
                .toBitmap(96, 96).asImageBitmap()
        }.getOrNull()
    }

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

                // A one-line recap of the trip (richer recap).
                if (stats.foundCount > 0) {
                    Text(
                        text = recapLine(stats),
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
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

                // The journey: states in the order they were found (richer recap).
                if (stats.timeline.isNotEmpty()) {
                    JourneySection(stats.timeline)
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

                // Share a long-screenshot image of this summary (playtest note #4).
                OutlinedButton(
                    onClick = { shareRequested = true },
                    enabled = uiState.mapShapes != null && !shareRequested,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Share,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.share_button))
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

        // Off-screen capture for sharing (playtest note #4): render the share card at full size,
        // record it into a graphics layer, snapshot it to a bitmap, and hand off to the share
        // sheet. Offset off the visible area so there's no on-screen flash.
        val shareShapes = uiState.mapShapes
        if (shareRequested && stats != null && shareShapes != null) {
            Box(
                modifier = Modifier
                    .width(360.dp)
                    // Unbounded so the full card is measured/captured even when it's taller than
                    // the screen; otherwise the parent clamps it and the bottom is cut off.
                    .wrapContentHeight(align = Alignment.Top, unbounded = true)
                    .offset(x = 5000.dp)
                    .drawWithContent {
                        // Record the (off-screen) card into the layer for capture, then draw it
                        // normally so the draw pass runs. It's positioned off the visible area, so
                        // nothing shows on screen; drawLayer isn't needed.
                        graphicsLayer.record { this@drawWithContent.drawContent() }
                        drawContent()
                    },
            ) {
                ShareableSummary(
                    stats = stats,
                    shapes = shareShapes,
                    foundCodes = uiState.foundCodes,
                    appIcon = appIcon,
                )
            }
            LaunchedEffect(Unit) {
                // Wait for the off-screen card to lay out and draw before snapshotting.
                withFrameNanos { }
                withFrameNanos { }
                val bitmap = graphicsLayer.toImageBitmap().asAndroidBitmap()
                shareTripImage(context, bitmap, shareChooserTitle)
                shareRequested = false
            }
        }
    }
}

private val SHARE_DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("MMMM d, yyyy")

/**
 * The non-scrolling card captured for sharing (playtest note #4): trip name, found count, the
 * filled map, the full stats, and a footer. Laid out at a fixed width on an opaque background so
 * the exported PNG looks right.
 */
@Composable
private fun ShareableSummary(
    stats: CelebrationStats,
    shapes: UsMapShapes,
    foundCodes: Set<String>,
    appIcon: ImageBitmap?,
) {
    val date = remember { LocalDate.now().format(SHARE_DATE_FORMAT) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stats.tripName,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.share_states_found, stats.foundCount),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth(),
        ) {
            UsMap(
                shapes = shapes,
                foundCodes = foundCodes,
                onStateClick = {},
                interactive = false,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(shapes.width / shapes.height)
                    .padding(12.dp),
            )
        }
        StatsSections(stats)
        // Branded watermark footer: app icon + "Made with License Plate Quest · {date}".
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(top = 4.dp),
        ) {
            if (appIcon != null) {
                Image(
                    bitmap = appIcon,
                    contentDescription = null,
                    modifier = Modifier
                        .size(24.dp)
                        .clip(RoundedCornerShape(6.dp)),
                )
            }
            Text(
                text = stringResource(R.string.share_footer, date),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
private fun recapLine(stats: CelebrationStats): String {
    val distance = stats.estimatedDistanceText
    return if (distance != null) {
        stringResource(
            R.string.celebration_recap_distance,
            stats.foundCount,
            stats.durationText,
            distance
        )
    } else {
        stringResource(R.string.celebration_recap, stats.foundCount, stats.durationText)
    }
}

/** "Your journey": the states in the order they were found, as a scrolling row of flag chips. */
@Composable
private fun JourneySection(timeline: List<TimelineFind>) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.celebration_journey_title),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(vertical = 4.dp),
        ) {
            itemsIndexed(timeline) { index, find ->
                Column(
                    modifier = Modifier.width(52.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    FlagImage(
                        code = find.code,
                        modifier = Modifier.width(48.dp),
                        placeholderFontSize = 13.sp,
                    )
                    Text(
                        text = "${index + 1}. ${find.code}",
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun StatsSections(stats: CelebrationStats) {
    val timing = listOf(
        Stat(stringResource(R.string.celebration_stat_average_gap), stats.averageGapText),
        Stat(stringResource(R.string.celebration_stat_longest_gap), stats.longestGapText),
        Stat(stringResource(R.string.celebration_stat_shortest_gap), stats.shortestGapText),
    )
    val highlights = listOf(
        Stat(stringResource(R.string.celebration_stat_first_state), stats.firstStateName),
        Stat(stringResource(R.string.celebration_stat_last_state), stats.lastStateName),
        Stat(stringResource(R.string.celebration_stat_busiest_day), stats.busiestDayText),
        Stat(stringResource(R.string.celebration_stat_furthest_state), stats.furthestStateName),
        Stat(stringResource(R.string.celebration_stat_rarest_state), stats.rarestStateName),
    )

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        StatTiles(stats)
        StatSection(stringResource(R.string.celebration_section_timing), timing)
        StatSection(stringResource(R.string.celebration_section_highlights), highlights)
        if (stats.leaderboard.isNotEmpty()) {
            LeaderboardSection(
                leaderboard = stats.leaderboard,
                unattributed = stats.unattributedCount,
            )
        }
    }
}

/** Per-player leaderboard with a crown for the lead, plus an unattributed line (playtest #18). */
@Composable
private fun LeaderboardSection(leaderboard: List<PlayerScore>, unattributed: Int) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.celebration_section_leaderboard),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
        )
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                leaderboard.forEachIndexed { index, player ->
                    if (index > 0) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                    LeaderboardRow(player)
                }
                if (unattributed > 0) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(R.string.celebration_unattributed),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = unattributed.toString(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LeaderboardRow(player: PlayerScore) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(PlayerColors.resolve(player.colorToken, player.id.toString())),
            )
            Text(
                text = player.name,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
            )
            if (player.isLeader) {
                Text(
                    text = stringResource(R.string.celebration_crown),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        Text(
            text = player.score.toString(),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

/** The headline numbers as a row of tiles (icon + big value + label) — the summary's hero stats. */
@Composable
private fun StatTiles(stats: CelebrationStats) {
    data class Tile(val icon: ImageVector, val value: String, val label: String)

    val tiles = buildList {
        add(
            Tile(
                Icons.Filled.Map,
                "${stats.foundCount} / 50",
                stringResource(R.string.celebration_stat_states_found),
            ),
        )
        add(
            Tile(
                Icons.Filled.Schedule,
                stats.durationText,
                stringResource(R.string.celebration_stat_trip_duration),
            ),
        )
        stats.estimatedDistanceText?.let {
            add(
                Tile(
                    Icons.Filled.DirectionsCar,
                    it,
                    stringResource(R.string.celebration_stat_estimated_distance)
                )
            )
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        tiles.forEach { tile ->
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        tile.icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                    Text(
                        text = tile.value,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                    )
                    Text(
                        text = tile.label,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
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
