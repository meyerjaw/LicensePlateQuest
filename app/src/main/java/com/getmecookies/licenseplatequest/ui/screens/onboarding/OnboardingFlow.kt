package com.getmecookies.licenseplatequest.ui.screens.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.getmecookies.licenseplatequest.R
import com.getmecookies.licenseplatequest.ui.AppViewModelProvider

/**
 * First-run onboarding wizard (skippable). Five steps — Welcome, Set home, Add players, Create
 * first trip, Ready — each its own screen with its own validation and a Back affordance. Data is
 * written through the real repositories as the user advances, so bailing never loses work; the
 * current step is persisted for force-quit resume. Finishing or skipping flips the completion flag
 * the app root observes, which swaps in the main app.
 */
@Composable
fun OnboardingFlow(
    viewModel: OnboardingViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // Mid-wizard back goes a step back rather than exiting the app; Welcome (step 0) consumes
    // nothing so the system back behaves normally there.
    BackHandler(enabled = state.step > 0) { viewModel.back() }

    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()) {
            // Progress + Back on the three input steps (Welcome and Ready are bookends).
            if (state.step in 1..OnboardingViewModel.LAST_STEP - 1) {
                OnboardingProgressBar(step = state.step, onBack = viewModel::back)
            }
            Crossfade(
                targetState = state.step,
                label = "onboarding-step",
                modifier = Modifier.weight(1f),
            ) { step ->
                when (step) {
                    0 -> WelcomeStep(onStart = viewModel::next, onSkip = viewModel::finish)
                    1 -> HomeStep(state, viewModel)
                    2 -> PlayersStep(state, viewModel)
                    3 -> TripStep(state, viewModel)
                    else -> ReadyStep(onDone = viewModel::finish)
                }
            }
        }
    }
}

/** A Back button + step dots + an announced "Step X of N" for screen readers. */
@Composable
private fun OnboardingProgressBar(step: Int, onBack: () -> Unit) {
    val stepLabel =
        stringResource(R.string.onb_step_indicator, step + 1, OnboardingViewModel.TOTAL_STEPS)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp)
            .semantics { contentDescription = stepLabel },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.onb_back),
            )
        }
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.Center,
        ) {
            for (i in 0 until OnboardingViewModel.TOTAL_STEPS) {
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .size(if (i == step) 10.dp else 8.dp)
                        .clip(CircleShape)
                        .background(
                            if (i <= step) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant,
                        ),
                )
            }
        }
        // Balance the Back button so the dots stay visually centered.
        Box(modifier = Modifier.size(48.dp))
    }
}
