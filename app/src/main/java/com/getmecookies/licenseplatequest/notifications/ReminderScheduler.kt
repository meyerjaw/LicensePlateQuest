package com.getmecookies.licenseplatequest.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.getmecookies.licenseplatequest.R
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import java.util.concurrent.TimeUnit
import androidx.core.content.edit

/**
 * Schedules and cancels the per-trip overdue reminder (playtest #13) via WorkManager. Backed by
 * unique work keyed on the trip id so create/edit/end/delete map cleanly to enqueue/replace/cancel.
 * WorkManager persists the queue across process death and reboot, so a scheduled reminder survives
 * without any boot receiver of our own.
 *
 * minSdk is 31, so the notification channel and exact-enough deferred work are always available;
 * no version guards are needed here.
 */
class ReminderScheduler(private val appContext: Context) {

    private val workManager: WorkManager get() = WorkManager.getInstance(appContext)

    // Tracks the end date we last notified for, per trip, so a trip only nudges once. Keyed on the
    // end date (not a bare flag) so a future "add days" action that moves the end date won't match
    // and a fresh reminder is allowed to fire.
    private val notifiedPrefs =
        appContext.getSharedPreferences(NOTIFIED_PREFS, Context.MODE_PRIVATE)

    /**
     * Enqueue the reminder for [tripId], firing [TripReminders.DAYS_AFTER_END] day(s) after
     * [endDate] at [TripReminders.REMINDER_HOUR]. If the computed time is already in the past
     * (e.g. an already-overdue trip), it runs as soon as constraints allow — the worker still
     * re-checks before posting. [replace] = false keeps an existing reminder untouched (used by
     * the startup reconcile so we don't reset already-queued work).
     */
    fun scheduleForTrip(tripId: UUID, endDate: LocalDate, replace: Boolean = true) {
        val fireAtMillis = endDate
            .plusDays(TripReminders.DAYS_AFTER_END)
            .atTime(TripReminders.REMINDER_HOUR, 0)
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        val delay = (fireAtMillis - System.currentTimeMillis()).coerceAtLeast(0L)

        val request = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(workDataOf(TripReminders.INPUT_TRIP_ID to tripId.toString()))
            .addTag(TripReminders.WORK_TAG)
            .build()

        workManager.enqueueUniqueWork(
            TripReminders.workName(tripId),
            if (replace) ExistingWorkPolicy.REPLACE else ExistingWorkPolicy.KEEP,
            request,
        )
    }

    /** Cancel any scheduled reminder for [tripId] (trip ended or deleted) and forget its state. */
    fun cancelForTrip(tripId: UUID) {
        workManager.cancelUniqueWork(TripReminders.workName(tripId))
        notifiedPrefs.edit { remove(tripId.toString()) }
    }

    /** True if we've already posted a reminder for [tripId] at its current [endDate]. */
    fun wasNotifiedFor(tripId: UUID, endDate: LocalDate): Boolean =
        notifiedPrefs.getString(tripId.toString(), null) == endDate.toString()

    /** Record that a reminder was posted for [tripId] at [endDate] so it won't fire again. */
    fun markNotified(tripId: UUID, endDate: LocalDate) {
        notifiedPrefs.edit { putString(tripId.toString(), endDate.toString()) }
    }

    /** Create (idempotently) the notification channel. Safe to call repeatedly. */
    fun ensureChannel() {
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

    private companion object {
        const val NOTIFIED_PREFS = "trip_reminder_notified"
    }
}
