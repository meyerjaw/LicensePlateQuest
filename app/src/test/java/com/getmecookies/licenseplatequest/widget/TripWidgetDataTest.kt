package com.getmecookies.licenseplatequest.widget

import org.junit.Assert.assertEquals
import org.junit.Test

/** Pure coverage for the widget's "time ago" label. */
class TripWidgetDataTest {

    private val now = 1_700_000_000_000L

    @Test
    fun relativeTime_formatsEachBucket() {
        assertEquals("just now", relativeTimeLabel(now - 30_000, now))
        assertEquals("12 min ago", relativeTimeLabel(now - 12 * 60_000, now))
        assertEquals("1 hr ago", relativeTimeLabel(now - 60 * 60_000, now))
        assertEquals("3 hr ago", relativeTimeLabel(now - 3 * 60 * 60_000, now))
        assertEquals("yesterday", relativeTimeLabel(now - 26L * 60 * 60_000, now))
        assertEquals("2 days ago", relativeTimeLabel(now - 50L * 60 * 60_000, now))
    }

    @Test
    fun relativeTime_clampsFutureToJustNow() {
        assertEquals("just now", relativeTimeLabel(now + 10_000, now))
    }
}
