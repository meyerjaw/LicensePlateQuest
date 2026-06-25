package com.getmecookies.licenseplatequest

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
import com.getmecookies.licenseplatequest.ui.xr.XrMapPanel
import com.getmecookies.licenseplatequest.widget.TripWidget
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.xr.compose.subspace.SpatialCurvedRow
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
                    Subspace {
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
