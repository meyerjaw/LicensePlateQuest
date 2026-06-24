package com.getmecookies.licenseplatequest

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.glance.appwidget.updateAll
import com.getmecookies.licenseplatequest.domain.model.ThemeMode
import com.getmecookies.licenseplatequest.notifications.TripReminders
import com.getmecookies.licenseplatequest.ui.navigation.AppRoot
import com.getmecookies.licenseplatequest.ui.screens.onboarding.OnboardingFlow
import com.getmecookies.licenseplatequest.ui.theme.LicensePlateQuestTheme
import com.getmecookies.licenseplatequest.widget.TripWidget
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
                if (!onboardingComplete) {
                    // First run (or a Settings-triggered restart): guide setup before the app shell.
                    OnboardingFlow()
                } else {
                    AppRoot(
                        editTripRequest = pendingEditTripId.value,
                        onEditTripRequestConsumed = { pendingEditTripId.value = null },
                    )
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
