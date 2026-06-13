package com.getmecookies.licenseplatequest.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure JVM tests for the consent gate: events pass through only while consent is on, and toggling
 * consent takes effect on the very next call (it's read live, not captured).
 */
class ConsentGatedAnalyticsTest {

    private val fake = FakeAnalytics()
    private var enabled = true
    private val gated = ConsentGatedAnalytics(fake) { enabled }

    @Test
    fun forwardsWhenEnabled() {
        gated.screen("trip_list")
        gated.event("trip_created", mapOf("player_count" to 2))
        gated.setUserProperty("has_completed_trip", "true")

        assertEquals(listOf("trip_list"), fake.screens.map { it.name })
        assertEquals(listOf("trip_created"), fake.eventNames())
        assertEquals(2, fake.paramsOf("trip_created")?.get("player_count"))
        assertEquals("true", fake.userProperties["has_completed_trip"])
    }

    @Test
    fun dropsEverythingWhenDisabled() {
        enabled = false

        gated.screen("trip_list")
        gated.event("trip_created")
        gated.setUserProperty("has_completed_trip", "true")

        assertTrue(fake.screens.isEmpty())
        assertTrue(fake.events.isEmpty())
        assertTrue(fake.userProperties.isEmpty())
    }

    @Test
    fun respectsLiveToggle() {
        gated.event("first") // enabled
        enabled = false
        gated.event("blocked")
        enabled = true
        gated.event("second")

        assertEquals(listOf("first", "second"), fake.eventNames())
    }
}
