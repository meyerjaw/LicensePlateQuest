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
 * Fires the overdue-trip reminder. Scheduled by [ReminderScheduler]; re-checks current state
 * before posting so stale jobs stay quiet (master setting off, trip gone/completed/no longer
 * overdue, notifications not granted). The primary run posts once per end date and schedules the
 * +3-day follow-up; the follow-up run bypasses the once-dedup.
 */
class ReminderWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val tripId = inputData.getString(TripReminders.INPUT_TRIP_ID)
            ?.let { runCatching { UUID.fromString(it) }.getOrNull() }
            ?: return Result.success()
        val isFollowUp = inputData.getBoolean(TripReminders.INPUT_IS_FOLLOWUP, false)

        val container = (applicationContext as LicensePlateQuestApp).container

        if (!container.settingsRepository.tripRemindersEnabled.value) return Result.success()

        val trip = container.tripRepository.getTrip(tripId) ?: return Result.success()
        val endDate = trip.endDate ?: return Result.success()
        if (trip.status == TripStatus.COMPLETED) return Result.success()
        if (!endDate.isBefore(LocalDate.now())) return Result.success()

        // The primary nudge fires once per end date; the follow-up deliberately bypasses that.
        if (!isFollowUp && container.reminderScheduler.wasNotifiedFor(tripId, endDate)) {
            return Result.success()
        }

        if (!hasNotificationPermission()) return Result.success()

        container.reminderScheduler.ensureChannel()
        postNotification(trip)

        if (!isFollowUp) {
            container.reminderScheduler.markNotified(tripId, endDate)
            container.reminderScheduler.scheduleFollowUp(tripId, endDate)
        }
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
        val tripId = trip.id
        val base = TripReminders.notificationId(tripId)

        val notification = NotificationCompat.Builder(applicationContext, TripReminders.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_reminder)
            .setContentTitle(applicationContext.getString(R.string.reminder_title))
            .setContentText(applicationContext.getString(R.string.reminder_text, trip.name))
            .setAutoCancel(true)
            .setContentIntent(openTripIntent(tripId, requestCode = base))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .addAction(
                0,
                applicationContext.getString(R.string.reminder_action_end),
                broadcastAction(TripReminders.ACTION_END_TRIP, tripId, requestCode = base + 1),
            )
            .addAction(
                0,
                applicationContext.getString(R.string.reminder_action_remind_later),
                broadcastAction(TripReminders.ACTION_REMIND_LATER, tripId, requestCode = base + 2),
            )
            .addAction(
                0,
                applicationContext.getString(R.string.reminder_action_extend),
                openTripIntent(tripId, requestCode = base + 3, openEdit = true),
            )
            .build()

        NotificationManagerCompat.from(applicationContext).notify(base, notification)
    }

    /** Tap (or Extend) intent into the app for this trip. */
    private fun openTripIntent(tripId: UUID, requestCode: Int, openEdit: Boolean = false): PendingIntent {
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(TripReminders.EXTRA_TRIP_ID, tripId.toString())
            if (openEdit) putExtra(TripReminders.EXTRA_OPEN_EDIT, true)
        }
        return PendingIntent.getActivity(
            applicationContext,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    /** Broadcast intent to [ReminderActionReceiver] for a notification action button. */
    private fun broadcastAction(action: String, tripId: UUID, requestCode: Int): PendingIntent {
        val intent = Intent(applicationContext, ReminderActionReceiver::class.java).apply {
            this.action = action
            putExtra(TripReminders.EXTRA_TRIP_ID, tripId.toString())
        }
        return PendingIntent.getBroadcast(
            applicationContext,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
