package com.getmecookies.licenseplatequest.notifications

import java.util.UUID

/**
 * Shared constants for the overdue-trip reminder feature (playtest #13). A reminder is a
 * one-off WorkManager job, uniquely named per trip so it can be replaced/cancelled when the
 * trip's end date changes, it's ended, or it's deleted. The worker re-checks the trip at fire
 * time, so toggling the setting off needs no cancellation — the worker simply no-ops.
 */
object TripReminders {
    /** Notification channel for overdue-trip nudges. */
    const val CHANNEL_ID = "trip_reminders"

    /** Prefix for the per-trip unique work name. */
    private const val WORK_NAME_PREFIX = "trip_reminder_"

    /** Tag on every reminder work request (handy for bulk inspection/cancel). */
    const val WORK_TAG = "trip_reminder"

    /** WorkManager input-data key carrying the trip id (as a string UUID). */
    const val INPUT_TRIP_ID = "trip_id"

    /** Intent extra used to deep-link a notification tap to its trip. */
    const val EXTRA_TRIP_ID = "com.getmecookies.licenseplatequest.EXTRA_TRIP_ID"

    /** Fire the reminder this many days after the trip's end date. */
    const val DAYS_AFTER_END = 1L

    /** Local hour-of-day (24h) the reminder fires, so it lands at a friendly time. */
    const val REMINDER_HOUR = 10

    fun workName(tripId: UUID): String = "$WORK_NAME_PREFIX$tripId"

    /** Stable per-trip notification id so a re-fire updates rather than stacks. */
    fun notificationId(tripId: UUID): Int = tripId.hashCode()
}
