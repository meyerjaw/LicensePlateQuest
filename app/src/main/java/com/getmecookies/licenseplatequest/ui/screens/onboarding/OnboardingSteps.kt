package com.getmecookies.licenseplatequest.ui.screens.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.getmecookies.licenseplatequest.R
import com.getmecookies.licenseplatequest.ui.PlayerColors
import com.getmecookies.licenseplatequest.ui.components.PlayerColorPicker
import com.getmecookies.licenseplatequest.ui.components.RegionPickerField

/** Welcome: app name + one-line pitch + Get started, with a corner Skip-setup escape hatch. */
@Composable
fun WelcomeStep(onStart: () -> Unit, onSkip: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.ic_empty_roadtrip),
            contentDescription = null,
            modifier = Modifier.size(180.dp),
        )
        Spacer(Modifier.size(24.dp))
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.size(8.dp))
        Text(
            text = stringResource(R.string.onb_welcome_pitch),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.size(32.dp))
        Button(onClick = onStart, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.onb_get_started))
        }
        TextButton(onClick = onSkip, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.onb_skip_setup))
        }
    }
}

/** Optional home: pre-fills the trip origin. Skippable. */
@Composable
fun HomeStep(state: OnboardingUiState, viewModel: OnboardingViewModel) {
    StepScaffold(
        title = stringResource(R.string.onb_home_title),
        subtitle = stringResource(R.string.onb_home_subtitle),
        primaryLabel = stringResource(R.string.onb_continue),
        onPrimary = viewModel::saveHomeAndNext,
        secondaryLabel = stringResource(R.string.onb_skip),
        onSecondary = viewModel::next,
    ) {
        RegionPickerField(
            label = stringResource(R.string.onb_home_state),
            options = state.regionOptions,
            selectedId = state.homeRegionId,
            onSelected = viewModel::onHomeRegion,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.size(12.dp))
        OutlinedTextField(
            value = state.homeCity,
            onValueChange = viewModel::onHomeCity,
            label = { Text(stringResource(R.string.onb_home_city)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** Add players inline (name + color); written immediately. Zero is allowed. */
@Composable
fun PlayersStep(state: OnboardingUiState, viewModel: OnboardingViewModel) {
    val colorDraft = state.playerColorDraft
        ?: PlayerColors.firstUnusedToken(state.players.map { it.colorToken })
    StepScaffold(
        title = stringResource(R.string.onb_players_title),
        subtitle = stringResource(R.string.onb_players_subtitle),
        primaryLabel = stringResource(R.string.onb_continue),
        onPrimary = viewModel::next,
        secondaryLabel = null,
        onSecondary = {},
    ) {
        // Already-added players.
        if (state.players.isEmpty()) {
            Text(
                text = stringResource(R.string.onb_players_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            state.players.forEach { player ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(
                                PlayerColors.resolve(
                                    player.colorToken,
                                    player.id.toString()
                                )
                            ),
                    )
                    Spacer(Modifier.size(12.dp))
                    Text(player.name, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
        Spacer(Modifier.size(16.dp))
        // Add-a-player row.
        OutlinedTextField(
            value = state.playerNameDraft,
            onValueChange = viewModel::onPlayerNameDraft,
            label = { Text(stringResource(R.string.onb_player_name)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.size(12.dp))
        PlayerColorPicker(
            selectedToken = colorDraft,
            onSelect = viewModel::onPlayerColorDraft,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.size(12.dp))
        Button(
            onClick = viewModel::addPlayer,
            enabled = state.playerDraftValid && !state.savingPlayer,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.size(8.dp))
            Text(stringResource(R.string.onb_player_add))
        }
    }
}

/** Create the first trip (origin pre-filled from home). Skippable. */
@Composable
fun TripStep(state: OnboardingUiState, viewModel: OnboardingViewModel) {
    StepScaffold(
        title = stringResource(R.string.onb_trip_title),
        subtitle = stringResource(R.string.onb_trip_subtitle),
        primaryLabel = stringResource(R.string.onb_trip_create),
        onPrimary = viewModel::createTripAndNext,
        primaryEnabled = state.tripValid && !state.savingTrip,
        secondaryLabel = stringResource(R.string.onb_trip_skip),
        onSecondary = viewModel::next,
    ) {
        OutlinedTextField(
            value = state.tripName,
            onValueChange = viewModel::onTripName,
            label = { Text(stringResource(R.string.onb_trip_name)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.size(12.dp))
        RegionPickerField(
            label = stringResource(R.string.onb_trip_from_state),
            options = state.regionOptions,
            selectedId = state.originRegionId,
            onSelected = viewModel::onOriginRegion,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.size(8.dp))
        OutlinedTextField(
            value = state.originCity,
            onValueChange = viewModel::onOriginCity,
            label = { Text(stringResource(R.string.onb_trip_from_city)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.size(16.dp))
        RegionPickerField(
            label = stringResource(R.string.onb_trip_to_state),
            options = state.regionOptions,
            selectedId = state.destRegionId,
            onSelected = viewModel::onDestRegion,
            excludeId = state.originRegionId,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.size(8.dp))
        OutlinedTextField(
            value = state.destCity,
            onValueChange = viewModel::onDestCity,
            label = { Text(stringResource(R.string.onb_trip_to_city)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** Ready: confirmation + Let's go. */
@Composable
fun ReadyStep(onDone: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(96.dp),
        )
        Spacer(Modifier.size(24.dp))
        Text(
            text = stringResource(R.string.onb_ready_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.size(8.dp))
        Text(
            text = stringResource(R.string.onb_ready_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.size(32.dp))
        Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.onb_ready_go))
        }
    }
}

/**
 * Shared chrome for the input steps: a scrollable title/subtitle + content area, with a pinned
 * primary button and an optional secondary (Skip) below it.
 */
@Composable
private fun StepScaffold(
    title: String,
    subtitle: String,
    primaryLabel: String,
    onPrimary: () -> Unit,
    secondaryLabel: String?,
    onSecondary: () -> Unit,
    primaryEnabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.size(8.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.size(24.dp))
            content()
            Spacer(Modifier.size(24.dp))
        }
        Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)) {
            Button(
                onClick = onPrimary,
                enabled = primaryEnabled,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(primaryLabel)
            }
            if (secondaryLabel != null) {
                TextButton(onClick = onSecondary, modifier = Modifier.fillMaxWidth()) {
                    Text(secondaryLabel)
                }
            }
        }
    }
}
