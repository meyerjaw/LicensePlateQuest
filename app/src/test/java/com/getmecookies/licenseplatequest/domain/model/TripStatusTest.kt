package com.getmecookies.licenseplatequest.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class TripStatusTest {

    @Test
    fun fromWire_roundTripsEveryValue() {
        TripStatus.entries.forEach { status ->
            assertEquals(status, TripStatus.fromWire(status.wire))
        }
    }

    @Test(expected = IllegalStateException::class)
    fun fromWire_throwsOnUnknown() {
        TripStatus.fromWire("not-a-real-status")
    }
}
