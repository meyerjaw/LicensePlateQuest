package com.getmecookies.licenseplatequest.notifications

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.getmecookies.licenseplatequest.LicensePlateQuestApp
import com.getmecookies.licenseplatequest.MainActivity
import com.getmecookies.licenseplatequest.R
import com.getmecookies.licenseplatequest.data.local.entity.TripEntity
import com.getmecookies.licenseplatequest.domain.model.TripStatus
import java.time.LocalDate
import java.util.UUID

/**
 * Fires the overdue-trip reminder. Scheduled by [ReminderScheduler] at end_date + N days, this
 * worker re-checks current state before posting so stale jobs stay quiet: it bails if the master
 * setting is off, the trip is gone, completed, no longer overdue, or notifications aren't granted.
 * Posting only when the condition still holds means toggling the setting off needs no cancellation.
 */
class ReminderWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val tripId = inputData.getString(TripReminders.INPUT_TRIP_ID)
            ?.let { runCatching { UUID.fromString(it) }.getOrNull() }
            ?: return Result.success()

        val container = (applicationContext as LicensePlateQuestApp).container

        // Master toggle, checked at fire time (so a flipped-off setting simply produces no nudge).
        if (!container.settingsRepository.tripRemindersEnabled.value) return Result.success()

        val trip = container.tripRepository.getTrip(tripId) ?: return Result.success()
        val endDate = trip.endDate ?: return Result.success()
        if (trip.status == TripStatus.COMPLETED) return Result.success()
        if (!endDate.isBefore(LocalDate.now())) return Result.success()

        // One nudge per trip per end date (re-fires only if the end date later moves).
        if (container.reminderScheduler.wasNotifiedFor(tripId, endDate)) return Result.success()

        if (!hasNotificationPermission()) return Result.success()

        container.reminderScheduler.ensureChannel()
        postNotification(trip)
        container.reminderScheduler.markNotified(tripId, endDate)
        return Result.success()
    }

    private fun hasNotificationPermission(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED

    // Permission is verified in doWork() before this is ever called.
    @SuppressLint("MissingPermission")
    private fun postNotification(trip: TripEntity) {
        val tapIntent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(TripReminders.EXTRA_TRIP_ID, trip.id.toString())
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            TripReminders.notificationId(trip.id),
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(applicationContext, TripReminders.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_reminder)
            .setContentTitle(applicationContext.getString(R.string.reminder_title))
            .setContentText(applicationContext.getString(R.string.reminder_text, trip.name))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        NotificationManagerCompat.from(applicationContext)
            .notify(TripReminders.notificationId(trip.id), notification)
    }
}
