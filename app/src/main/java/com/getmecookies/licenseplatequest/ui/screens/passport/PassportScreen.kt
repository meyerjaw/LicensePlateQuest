package com.getmecookies.licenseplatequest.ui.screens.passport

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.getmecookies.licenseplatequest.R
import com.getmecookies.licenseplatequest.domain.Achievement
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

        AchievementsSection(earned = uiState.earnedAchievements)

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
                badgeLabel = when {
                    state.code in uiState.rareCodes -> stringResource(R.string.state_rare_badge)
                    state.code in uiState.newToCollection -> stringResource(R.string.passport_new_badge)
                    else -> null
                },
                onClick = { onOpenState(state.code) },
            )
        }
    }
}

/** Earned vs locked achievement badges, with a header showing earned/total (playtest: achievements). */
@Composable
private fun AchievementsSection(earned: Set<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.ach_section_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = stringResource(R.string.ach_progress, earned.size, Achievement.entries.size),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Achievement.entries.chunked(2).forEach { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                pair.forEach { achievement ->
                    AchievementBadge(
                        achievement = achievement,
                        earned = achievement.id in earned,
                        modifier = Modifier.weight(1f),
                    )
                }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun AchievementBadge(
    achievement: Achievement,
    earned: Boolean,
    modifier: Modifier = Modifier
) {
    val meta = achievementMeta(achievement)
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = if (earned) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainerHighest
            },
        ),
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = meta.icon,
                contentDescription = if (earned) null else stringResource(R.string.ach_locked_cd),
                tint = if (earned) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier
                    .size(28.dp)
                    .alpha(if (earned) 1f else 0.45f),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(meta.titleRes),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    color = if (earned) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
                Text(
                    text = stringResource(meta.descRes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                )
            }
        }
    }
}

private val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy")
