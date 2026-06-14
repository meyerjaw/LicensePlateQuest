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

    /** Prefix for the per-trip follow-up unique work name (the second, +3-day nudge). */
    private const val FOLLOWUP_WORK_NAME_PREFIX = "trip_reminder_followup_"

    /** Tag on every reminder work request (handy for bulk inspection/cancel). */
    const val WORK_TAG = "trip_reminder"

    /** WorkManager input-data key carrying the trip id (as a string UUID). */
    const val INPUT_TRIP_ID = "trip_id"

    /** WorkManager input-data flag: true for the +3-day follow-up run (skips the once dedup). */
    const val INPUT_IS_FOLLOWUP = "is_followup"

    /** Intent extra used to deep-link a notification tap to its trip. */
    const val EXTRA_TRIP_ID = "com.getmecookies.licenseplatequest.EXTRA_TRIP_ID"

    /** Intent extra: open the Manage trip (edit) screen for the trip rather than just viewing it. */
    const val EXTRA_OPEN_EDIT = "com.getmecookies.licenseplatequest.EXTRA_OPEN_EDIT"

    /** Notification action: finalize the trip directly. */
    const val ACTION_END_TRIP = "com.getmecookies.licenseplatequest.ACTION_END_TRIP"

    /** Notification action: snooze the reminder a couple of days. */
    const val ACTION_REMIND_LATER = "com.getmecookies.licenseplatequest.ACTION_REMIND_LATER"

    /** Fire the first reminder this many days after the trip's end date. */
    const val DAYS_AFTER_END = 1L

    /** Fire the follow-up nudge this many days after the trip's end date. */
    const val DAYS_AFTER_END_FOLLOWUP = 3L

    /** "Remind later" snoozes the reminder this many days from now. */
    const val REMIND_LATER_DAYS = 2L

    /** Local hour-of-day (24h) the reminder fires, so it lands at a friendly time. */
    const val REMINDER_HOUR = 10

    /** Analytics label for the "Extend" deep-link (an activity intent, not a broadcast action). */
    const val ACTION_LABEL_EXTEND = "extend"

    /**
     * The analytics `action` label for a reminder notification button, or null if [action] isn't a
     * tracked reminder action. Pure mapping so it's unit-testable without the receiver/Android.
     */
    fun actionLabel(action: String?): String? = when (action) {
        ACTION_END_TRIP -> "end"
        ACTION_REMIND_LATER -> "remind"
        else -> null
    }

    fun workName(tripId: UUID): String = "$WORK_NAME_PREFIX$tripId"

    fun followUpWorkName(tripId: UUID): String = "$FOLLOWUP_WORK_NAME_PREFIX$tripId"

    /** Stable per-trip notification id so a re-fire updates rather than stacks. */
    fun notificationId(tripId: UUID): Int = tripId.hashCode()
}
