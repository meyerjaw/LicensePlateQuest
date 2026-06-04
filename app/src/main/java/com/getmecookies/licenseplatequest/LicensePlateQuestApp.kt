package com.getmecookies.licenseplatequest

import android.app.Application
import com.getmecookies.licenseplatequest.di.AppContainer
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
        }
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
