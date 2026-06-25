package com.getmecookies.licenseplatequest

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.glance.appwidget.updateAll
import androidx.xr.compose.platform.LocalSpatialCapabilities
import androidx.xr.compose.spatial.Subspace
import androidx.xr.compose.subspace.SpatialPanel
import androidx.xr.compose.subspace.layout.SubspaceModifier
import androidx.xr.compose.subspace.layout.height
import androidx.xr.compose.subspace.layout.width
import com.getmecookies.licenseplatequest.domain.model.ThemeMode
import com.getmecookies.licenseplatequest.notifications.TripReminders
import com.getmecookies.licenseplatequest.ui.navigation.AppRoot
import com.getmecookies.licenseplatequest.ui.screens.onboarding.OnboardingFlow
import com.getmecookies.licenseplatequest.ui.theme.LicensePlateQuestTheme
import com.getmecookies.licenseplatequest.ui.xr.XrCelebrationOverlay
import com.getmecookies.licenseplatequest.ui.xr.XrMapPanel
import com.getmecookies.licenseplatequest.ui.xr.XrTrophyShelf
import com.getmecookies.licenseplatequest.widget.TripWidget
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.xr.compose.subspace.SpatialBox
import androidx.xr.compose.subspace.SpatialColumn
import androidx.xr.compose.subspace.SpatialCurvedRow
import androidx.xr.compose.subspace.layout.offset
import androidx.xr.compose.subspace.layout.padding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Single-activity host (SPEC §9 — Jetpack Compose). All screens are composables behind the
 * navigation graph in [AppRoot]. The theme follows the user's Settings choice (live).
 */
class MainActivity : ComponentActivity() {

    // Set when a notification's "Extend" action launches us; consumed by AppRoot to open the
    // Manage trip screen for that trip.
    private val pendingEditTripId = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleReminderIntent(intent)
        val container = (application as LicensePlateQuestApp).container
        val settings = container.settingsRepository
        setContent {
            val themeMode by settings.themeMode.collectAsStateWithLifecycle()
            val darkTheme = when (themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            val onboardingComplete by container.uiPreferences.onboardingComplete
                .collectAsStateWithLifecycle()
            LicensePlateQuestTheme(darkTheme = darkTheme) {
                // The whole app shell (onboarding or the main nav). Reused as-is in both the flat
                // (phone/tablet) and spatial (Android XR) presentations.
                val appShell = @Composable {
                    if (!onboardingComplete) {
                        // First run (or a Settings restart): guide setup before the app shell.
                        OnboardingFlow()
                    } else {
                        AppRoot(
                            editTripRequest = pendingEditTripId.value,
                            onEditTripRequestConsumed = { pendingEditTripId.value = null },
                        )
                    }
                }

                // On an Android XR headset in Full Space, lay out a curved "cockpit": the interactive
                // app on one panel and a big dedicated US map curving alongside it (reflecting the
                // active trip's finds live). Everywhere else (phones/tablets, or XR Home Space) render
                // the same shell flat — no separate XR UI to maintain. (Experimental: Jetpack XR.)
                if (LocalSpatialCapabilities.current.isSpatialUiEnabled) {
                    val foundCodes by container.spottingRepository
                        .observeFoundCodesForActiveTrip()
                        .collectAsStateWithLifecycle(emptySet())
                    val earnedTrophies by container.achievementRepository
                        .observeEarned()
                        .collectAsStateWithLifecycle(emptySet())

                    // Spatial confetti trigger: collect the active trip's found set directly and bump
                    // the trigger only when it GROWS after the first (baseline) emission — so it never
                    // fires on app load (the initial emission only seeds the baseline; the artificial
                    // emptySet from collectAsState would have looked like growth).
                    // confettiActive keeps the input-capturing confetti panel mounted only during the
                    // burst, so it doesn't block taps on the app the rest of the time.
                    var confettiTrigger by remember { mutableIntStateOf(0) }
                    var confettiActive by remember { mutableStateOf(false) }
                    LaunchedEffect(Unit) {
                        var baseline: Int? = null
                        container.spottingRepository.observeFoundCodesForActiveTrip()
                            .collect { codes ->
                                val prev = baseline
                                if (prev != null && codes.size > prev) confettiTrigger++
                                baseline = codes.size
                            }
                    }
                    LaunchedEffect(confettiTrigger) {
                        if (confettiTrigger > 0) {
                            confettiActive = true
                            delay(2900L)
                            confettiActive = false
                        }
                    }

                    Subspace {
                        SpatialBox {
                            SpatialColumn {
                                SpatialCurvedRow(curveRadius = 1400.dp) {
                                    SpatialPanel(
                                        modifier = SubspaceModifier
                                            .width(820.dp)
                                            .height(720.dp),
                                    ) {
                                        appShell()
                                    }
                                    SpatialPanel(
                                        modifier = SubspaceModifier
                                            .width(1100.dp)
                                            .height(720.dp),
                                    ) {
                                        XrMapPanel(
                                            mapRepository = container.mapRepository,
                                            foundCodes = foundCodes,
                                            modifier = Modifier.fillMaxSize(),
                                        )
                                    }
                                }
                                // Earned achievements float as a trophy shelf below the cockpit.
                                XrTrophyShelf(
                                    earnedAchievements = earnedTrophies,
                                    modifier = SubspaceModifier.padding(top = 32.dp),
                                )
                            }
                            // Transparent confetti panel, pulled toward the viewer (+z) so it rains in
                            // FRONT of the cockpit. Only mounted during the burst — an XR panel
                            // captures input across its whole quad, so leaving it up would block taps
                            // on the app. (If the burst appears *behind* the windows, flip the z sign.)
                            if (confettiActive) {
                                SpatialPanel(
                                    modifier = SubspaceModifier
                                        .width(1600.dp)
                                        .height(1100.dp)
                                        .offset(z = 250.dp),
                                ) {
                                    XrCelebrationOverlay(trigger = confettiTrigger)
                                }
                            }
                        }
                    }
                } else {
                    appShell()
                }
            }
        }
    }

    // Refresh the home-screen widget as the app goes to the background — i.e. right before the user
    // looks at the home screen — so it reflects the latest finds. The process is still alive here, so
    // the update runs promptly; failures are ignored. This (plus the provider's periodic refresh)
    // replaces the fragile always-on Application observer.
    override fun onStop() {
        super.onStop()
        lifecycleScope.launch { runCatching { TripWidget().updateAll(this@MainActivity) } }
    }

    // singleTop: a notification tap on the already-running app arrives here rather than onCreate.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleReminderIntent(intent)
    }

    /**
     * Handle an overdue-trip reminder launch (playtest #13/#14). The "Extend" action opens the
     * Manage trip screen; a plain tap makes the trip active so it surfaces on the Trips tab.
     * Consume the extras so they aren't re-applied on recreation.
     */
    private fun handleReminderIntent(intent: Intent?) {
        val tripIdString = intent?.getStringExtra(TripReminders.EXTRA_TRIP_ID) ?: return
        val tripId = runCatching { UUID.fromString(tripIdString) }.getOrNull() ?: return
        val openEdit = intent.getBooleanExtra(TripReminders.EXTRA_OPEN_EDIT, false)
        intent.removeExtra(TripReminders.EXTRA_TRIP_ID)
        intent.removeExtra(TripReminders.EXTRA_OPEN_EDIT)

        val container = (application as LicensePlateQuestApp).container
        if (openEdit) {
            container.analytics.event(
                "reminder_action",
                mapOf("action" to TripReminders.ACTION_LABEL_EXTEND),
            )
            pendingEditTripId.value = tripIdString
        } else {
            lifecycleScope.launch {
                container.tripRepository.setActiveTrip(tripId)
            }
        }
    }
}
