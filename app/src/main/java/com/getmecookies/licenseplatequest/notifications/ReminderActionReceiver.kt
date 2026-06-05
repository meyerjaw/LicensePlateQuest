package com.getmecookies.licenseplatequest.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.getmecookies.licenseplatequest.LicensePlateQuestApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Handles the overdue-trip notification action buttons (playtest #13/#14):
 * - End trip: finalize the trip directly (which also cancels its reminders).
 * - Remind later: snooze the nudge a couple of days.
 *
 * Registered in the manifest (not exported). "Extend" is an activity intent, handled by
 * MainActivity, not here.
 */
class ReminderActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val tripId = intent.getStringExtra(TripReminders.EXTRA_TRIP_ID)
            ?.let { runCatching { UUID.fromString(it) }.getOrNull() }
            ?: return
        val container = (context.applicationContext as LicensePlateQuestApp).container

        // Dismiss the notification either way.
        NotificationManagerCompat.from(context).cancel(TripReminders.notificationId(tripId))

        when (intent.action) {
            TripReminders.ACTION_END_TRIP -> {
                // endTrip is suspend + touches the DB, so finish the broadcast asynchronously.
                val pending = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        container.tripRepository.endTrip(tripId) // also cancels the reminders
                    } finally {
                        pending.finish()
                    }
                }
            }

            TripReminders.ACTION_REMIND_LATER -> {
                container.reminderScheduler.remindLater(tripId)
            }
        }
    }
}
