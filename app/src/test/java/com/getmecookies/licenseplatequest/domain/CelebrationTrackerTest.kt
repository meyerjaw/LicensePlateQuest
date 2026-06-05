package com.getmecookies.licenseplatequest.domain

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.UUID

/**
 * Tests the "50/50 celebration fires exactly once per trip" rule (SPEC §10): re-marking the 50th
 * state must not re-trigger it. Backed by SharedPreferences, so exercised under Robolectric.
 */
@RunWith(RobolectricTestRunner::class)
class CelebrationTrackerTest {

    private lateinit var tracker: CelebrationTracker

    @Before
    fun setUp() {
        tracker = CelebrationTracker(ApplicationProvider.getApplicationContext<Context>())
    }

    @Test
    fun fiftyCelebration_firesOncePerTrip() {
        val trip = UUID.randomUUID()
        assertFalse(tracker.hasCelebratedFifty(trip))

        tracker.markFiftyCelebrated(trip)

        assertTrue(tracker.hasCelebratedFifty(trip))
    }

    @Test
    fun fiftyCelebration_isTrackedPerTrip() {
        val celebrated = UUID.randomUUID()
        val other = UUID.randomUUID()

        tracker.markFiftyCelebrated(celebrated)

        assertTrue(tracker.hasCelebratedFifty(celebrated))
        assertFalse(tracker.hasCelebratedFifty(other))
    }

    @Test
    fun markingTwice_isIdempotent() {
        val trip = UUID.randomUUID()

        tracker.markFiftyCelebrated(trip)
        tracker.markFiftyCelebrated(trip)

        assertTrue(tracker.hasCelebratedFifty(trip))
    }
}
