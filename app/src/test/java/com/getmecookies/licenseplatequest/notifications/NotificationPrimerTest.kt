package com.getmecookies.licenseplatequest.notifications

import org.junit.Assert.assertEquals
import org.junit.Test

/** Pure-JVM coverage for the notification pre-prompt decision + snooze logic. */
class NotificationPrimerTest {

    @Test
    fun noRuntimePermissionNeeded_isNone() {
        assertEquals(
            NotificationPrimerAction.NONE,
            notificationPrimerAction(
                needsRuntimePermission = false,
                granted = false,
                hasRequestedBefore = true,
                shouldShowRationale = false,
            ),
        )
    }

    @Test
    fun alreadyGranted_isNone() {
        assertEquals(
            NotificationPrimerAction.NONE,
            notificationPrimerAction(
                true,
                granted = true,
                hasRequestedBefore = true,
                shouldShowRationale = true
            ),
        )
    }

    @Test
    fun neverAsked_showsPrimer() {
        assertEquals(
            NotificationPrimerAction.SHOW_PRIMER,
            notificationPrimerAction(
                true,
                granted = false,
                hasRequestedBefore = false,
                shouldShowRationale = false
            ),
        )
    }

    @Test
    fun deniedOnce_stillShowsPrimer() {
        // Denied but the OS will still show the dialog (rationale true) -> we can re-ask.
        assertEquals(
            NotificationPrimerAction.SHOW_PRIMER,
            notificationPrimerAction(
                true,
                granted = false,
                hasRequestedBefore = true,
                shouldShowRationale = true
            ),
        )
    }

    @Test
    fun permanentlyDenied_showsSettings() {
        // Asked before and the OS won't show the dialog -> deep-link to settings.
        assertEquals(
            NotificationPrimerAction.SHOW_SETTINGS,
            notificationPrimerAction(
                true,
                granted = false,
                hasRequestedBefore = true,
                shouldShowRationale = false
            ),
        )
    }

    @Test
    fun forcedTrigger_ignoresSnooze() {
        assertEquals(
            SnoozeDecision(skip = false, nextSnooze = 2),
            resolveSnooze(force = true, snoozeRemaining = 2)
        )
    }

    @Test
    fun snoozeConsumesOneTriggerAtATime() {
        assertEquals(
            SnoozeDecision(skip = true, nextSnooze = 1),
            resolveSnooze(force = false, snoozeRemaining = 2)
        )
        assertEquals(
            SnoozeDecision(skip = true, nextSnooze = 0),
            resolveSnooze(force = false, snoozeRemaining = 1)
        )
        assertEquals(
            SnoozeDecision(skip = false, nextSnooze = 0),
            resolveSnooze(force = false, snoozeRemaining = 0)
        )
    }
}
