package com.getmecookies.licenseplatequest.notifications

/** What to do when a notification-gated action is triggered (pre-permission priming). */
enum class NotificationPrimerAction {
    /** Already granted, or no runtime permission needed — proceed silently. */
    NONE,

    /** Show our rationale primer; on accept, launch the system permission dialog. */
    SHOW_PRIMER,

    /** Permanently denied — the system dialog won't reshow; offer a deep-link to Settings. */
    SHOW_SETTINGS,
}

/** How many subsequent (non-forced) triggers to skip after the user taps "Not now". */
const val NOTIFICATION_PRIMER_SNOOZE = 2

/**
 * Pure decision for the notification pre-prompt (playtest #13 follow-up). Framework-free so it can
 * be unit-tested; the composable layer supplies the runtime values (SDK level, grant status, the
 * "asked before" flag, and `shouldShowRequestPermissionRationale`).
 */
fun notificationPrimerAction(
    needsRuntimePermission: Boolean,
    granted: Boolean,
    hasRequestedBefore: Boolean,
    shouldShowRationale: Boolean,
): NotificationPrimerAction = when {
    !needsRuntimePermission || granted -> NotificationPrimerAction.NONE
    // Asked before, yet the OS now refuses to show the dialog => permanently denied.
    hasRequestedBefore && !shouldShowRationale -> NotificationPrimerAction.SHOW_SETTINGS
    else -> NotificationPrimerAction.SHOW_PRIMER
}

/** The outcome of applying snooze to a trigger: whether to skip, and the snooze value to persist. */
data class SnoozeDecision(val skip: Boolean, val nextSnooze: Int)

/**
 * Apply the post-"Not now" snooze. A [force]d trigger (an explicit "enable reminders" action)
 * ignores snooze; otherwise a positive [snoozeRemaining] is consumed one trigger at a time.
 */
fun resolveSnooze(force: Boolean, snoozeRemaining: Int): SnoozeDecision = when {
    force -> SnoozeDecision(skip = false, nextSnooze = snoozeRemaining)
    snoozeRemaining > 0 -> SnoozeDecision(skip = true, nextSnooze = snoozeRemaining - 1)
    else -> SnoozeDecision(skip = false, nextSnooze = 0)
}
