package com.getmecookies.licenseplatequest.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.content.edit
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.getmecookies.licenseplatequest.R
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * Schedules/cancels the per-trip overdue reminder (playtest #13) and tracks one-nudge-per-trip
 * state. Defined as an interface so tests can substitute a fake without touching WorkManager;
 * the production implementation is [WorkManagerReminderScheduler].
 */
interface ReminderScheduler {
    /** Enqueue (or replace) the primary reminder for [tripId], firing ~1 day after [endDate]. */
    fun scheduleForTrip(tripId: UUID, endDate: LocalDate, replace: Boolean = true)

    /** Enqueue the +3-day follow-up nudge (scheduled by the worker after the primary fires). */
    fun scheduleFollowUp(tripId: UUID, endDate: LocalDate)

    /** Snooze: clear the notified flag and re-fire the primary reminder a couple of days out. */
    fun remindLater(tripId: UUID)

    /** Cancel both the primary and follow-up reminders for [tripId] and forget notified state. */
    fun cancelForTrip(tripId: UUID)

    /** True if the primary reminder was already posted for [tripId] at its current [endDate]. */
    fun wasNotifiedFor(tripId: UUID, endDate: LocalDate): Boolean

    /** Record that the primary reminder was posted for [tripId] at [endDate]. */
    fun markNotified(tripId: UUID, endDate: LocalDate)

    /** Create (idempotently) the notification channel. */
    fun ensureChannel()
}

/**
 * WorkManager-backed [ReminderScheduler]. Unique work keyed on the trip id maps create/edit/end/
 * delete cleanly to enqueue/replace/cancel; WorkManager persists the queue across process death
 * and reboot, so a scheduled reminder survives without any boot receiver of our own.
 *
 * minSdk is 31, so the notification channel and deferred work are always available; no version
 * guards are needed here.
 */
class WorkManagerReminderScheduler(private val appContext: Context) : ReminderScheduler {

    private val workManager: WorkManager get() = WorkManager.getInstance(appContext)

    // Tracks the end date we last notified for, per trip, so a trip only nudges once. Keyed on the
    // end date (not a bare flag) so a future "add days" action that moves the end date won't match
    // and a fresh reminder is allowed to fire.
    private val notifiedPrefs =
        appContext.getSharedPreferences(NOTIFIED_PREFS, Context.MODE_PRIVATE)

    override fun scheduleForTrip(tripId: UUID, endDate: LocalDate, replace: Boolean) {
        enqueue(
            workName = TripReminders.workName(tripId),
            tripId = tripId,
            delayMillis = delayUntil(endDate, TripReminders.DAYS_AFTER_END),
            isFollowUp = false,
            policy = if (replace) ExistingWorkPolicy.REPLACE else ExistingWorkPolicy.KEEP,
        )
    }

    override fun scheduleFollowUp(tripId: UUID, endDate: LocalDate) {
        enqueue(
            workName = TripReminders.followUpWorkName(tripId),
            tripId = tripId,
            delayMillis = delayUntil(endDate, TripReminders.DAYS_AFTER_END_FOLLOWUP),
            isFollowUp = true,
            policy = ExistingWorkPolicy.REPLACE,
        )
    }

    override fun remindLater(tripId: UUID) {
        // Clear the notified flag so the rescheduled primary is allowed to post again.
        notifiedPrefs.edit { remove(tripId.toString()) }
        enqueue(
            workName = TripReminders.workName(tripId),
            tripId = tripId,
            delayMillis = TimeUnit.DAYS.toMillis(TripReminders.REMIND_LATER_DAYS),
            isFollowUp = false,
            policy = ExistingWorkPolicy.REPLACE,
        )
    }

    override fun cancelForTrip(tripId: UUID) {
        workManager.cancelUniqueWork(TripReminders.workName(tripId))
        workManager.cancelUniqueWork(TripReminders.followUpWorkName(tripId))
        notifiedPrefs.edit { remove(tripId.toString()) }
    }

    override fun wasNotifiedFor(tripId: UUID, endDate: LocalDate): Boolean =
        notifiedPrefs.getString(tripId.toString(), null) == endDate.toString()

    override fun markNotified(tripId: UUID, endDate: LocalDate) {
        notifiedPrefs.edit { putString(tripId.toString(), endDate.toString()) }
    }

    override fun ensureChannel() {
        val channel = NotificationChannel(
            TripReminders.CHANNEL_ID,
            appContext.getString(R.string.reminder_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = appContext.getString(R.string.reminder_channel_desc)
        }
        appContext.getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)
    }

    private fun enqueue(
        workName: String,
        tripId: UUID,
        delayMillis: Long,
        isFollowUp: Boolean,
        policy: ExistingWorkPolicy,
    ) {
        val request = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .setInputData(
                workDataOf(
                    TripReminders.INPUT_TRIP_ID to tripId.toString(),
                    TripReminders.INPUT_IS_FOLLOWUP to isFollowUp,
                ),
            )
            .addTag(TripReminders.WORK_TAG)
            .build()
        workManager.enqueueUniqueWork(workName, policy, request)
    }

    private fun delayUntil(endDate: LocalDate, daysAfter: Long): Long {
        val fireAtMillis = endDate
            .plusDays(daysAfter)
            .atTime(TripReminders.REMINDER_HOUR, 0)
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        return (fireAtMillis - System.currentTimeMillis()).coerceAtLeast(0L)
    }

    private companion object {
        const val NOTIFIED_PREFS = "trip_reminder_notified"
    }
}
