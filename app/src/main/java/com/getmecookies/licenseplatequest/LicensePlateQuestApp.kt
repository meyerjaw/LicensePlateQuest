package com.getmecookies.licenseplatequest

import android.app.Application
import com.getmecookies.licenseplatequest.di.AppContainer
import com.getmecookies.licenseplatequest.domain.UserProperties
import com.getmecookies.licenseplatequest.domain.model.TripStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Application entry point. Builds the [AppContainer] and kicks off bundled-data seeding
 * off the main thread on startup (SPEC §11 — bundled state data loading).
 */
class LicensePlateQuestApp : Application() {

    lateinit var container: AppContainer
        private set

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        applicationScope.launch {
            container.regionSeeder.seedIfNeeded()
            reconcileTripReminders()
            syncAnalyticsUserProperties()
        }
        // The home-screen widget is refreshed when the app goes to the background (MainActivity.onStop)
        // — i.e. right before the user looks at the home screen — plus the provider XML's periodic
        // refresh. We deliberately do NOT drive it from a long-lived Application observer: the system
        // restarts this process headlessly to run widget sessions, which would re-fire the observer
        // and create a churn of cancelling Glance sessions.
        // Keep Firebase's collection switch in lock-step with the consent setting (handles the
        // SDK's automatic events; our explicit events are also gated by ConsentGatedAnalytics).

        applicationScope.launch {
            container.settingsRepository.analyticsEnabled.collect { enabled ->
                container.applyAnalyticsConsent(enabled)
            }
        }
    }

    /**
     * Refresh the non-PII analytics cohort properties on each launch (consent-gated like every
     * event). Bucketed counts only — see [UserProperties]. theme_pref reflects the value at launch;
     * an in-session theme change takes effect on the next launch, which is fine for cohorting.
     */
    private suspend fun syncAnalyticsUserProperties() {
        val playerCount = container.playerRepository.observePlayers().first().size
        val stats = container.achievementRepository.getStats()
        UserProperties.apply(
            analytics = container.analytics,
            playerCount = playerCount,
            hasCompletedTrip = stats.completedTripCount > 0,
            lifetimeStatesFound = stats.lifetimeFound.size,
            theme = container.settingsRepository.themeMode.value,
        )
    }

    /**
     * Re-arm overdue reminders for any non-completed trip that has an end date (playtest #13).
     * WorkManager already persists scheduled work across reboot/process death, so this only fills
     * gaps — e.g. trips that had an end date before reminders existed, or a restored backup. Uses
     * KEEP so it never disturbs reminders that are already queued.
     */
    private suspend fun reconcileTripReminders() {
        container.reminderScheduler.ensureChannel()
        container.tripRepository.observeTripListItems().first().forEach { item ->
            val endDate = item.endDate
            if (endDate != null &&
                item.status != TripStatus.COMPLETED &&
                !container.reminderScheduler.wasNotifiedFor(item.id, endDate)
            ) {
                container.reminderScheduler.scheduleForTrip(item.id, endDate, replace = false)
            }
        }
    }
}
