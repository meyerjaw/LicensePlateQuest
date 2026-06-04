package com.getmecookies.licenseplatequest

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.getmecookies.licenseplatequest.domain.model.ThemeMode
import com.getmecookies.licenseplatequest.notifications.TripReminders
import com.getmecookies.licenseplatequest.ui.navigation.AppRoot
import com.getmecookies.licenseplatequest.ui.theme.LicensePlateQuestTheme
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Single-activity host (SPEC §9 — Jetpack Compose). All screens are composables behind the
 * navigation graph in [AppRoot]. The theme follows the user's Settings choice (live).
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleReminderIntent(intent)
        val settings = (application as LicensePlateQuestApp).container.settingsRepository
        setContent {
            val themeMode by settings.themeMode.collectAsStateWithLifecycle()
            val darkTheme = when (themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            LicensePlateQuestTheme(darkTheme = darkTheme) {
                AppRoot()
            }
        }
    }

    // singleTop: a notification tap on the already-running app arrives here rather than onCreate.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleReminderIntent(intent)
    }

    /**
     * If launched from an overdue-trip reminder (playtest #13), make that trip the active one so
     * it surfaces on the Trips tab. Consume the extra so it isn't re-applied on recreation.
     */
    private fun handleReminderIntent(intent: Intent?) {
        val tripId = intent?.getStringExtra(TripReminders.EXTRA_TRIP_ID)
            ?.let { runCatching { UUID.fromString(it) }.getOrNull() }
            ?: return
        intent.removeExtra(TripReminders.EXTRA_TRIP_ID)
        val container = (application as LicensePlateQuestApp).container
        lifecycleScope.launch {
            container.tripRepository.setActiveTrip(tripId)
        }
    }
}
