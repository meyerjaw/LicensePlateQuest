package com.getmecookies.licenseplatequest.notifications

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Pure mapping coverage for [TripReminders.actionLabel] — the analytics `action` label for each
 * reminder notification button (drives the `reminder_action` event). No Android needed.
 */
class TripRemindersTest {

    @Test
    fun actionLabel_mapsEndTrip() {
        assertEquals("end", TripReminders.actionLabel(TripReminders.ACTION_END_TRIP))
    }

    @Test
    fun actionLabel_mapsRemindLater() {
        assertEquals("remind", TripReminders.actionLabel(TripReminders.ACTION_REMIND_LATER))
    }

    @Test
    fun actionLabel_returnsNullForUntrackedActions() {
        assertNull(TripReminders.actionLabel(null))
        assertNull(TripReminders.actionLabel("android.intent.action.VIEW"))
    }
}
