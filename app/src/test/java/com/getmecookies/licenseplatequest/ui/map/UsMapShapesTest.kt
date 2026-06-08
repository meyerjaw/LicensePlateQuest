package com.getmecookies.licenseplatequest.ui.map

import androidx.compose.ui.geometry.Offset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Pure-JVM coverage for the small-state tap tolerance ([nearestCodeWithin]). */
class UsMapShapesTest {

    private val anchors = listOf(
        "RI" to Offset(100f, 100f),
        "CT" to Offset(130f, 100f),
        "DE" to Offset(300f, 300f),
    )

    @Test
    fun returnsNearestWithinTolerance() {
        // Closer to RI than CT.
        assertEquals("RI", nearestCodeWithin(105f, 100f, anchors, tolerance = 20f))
    }

    @Test
    fun picksTheClosestWhenSeveralAreInRange() {
        // Between RI(100) and CT(130) but nearer CT.
        assertEquals("CT", nearestCodeWithin(122f, 100f, anchors, tolerance = 40f))
    }

    @Test
    fun returnsNullWhenNothingWithinTolerance() {
        assertNull(nearestCodeWithin(500f, 500f, anchors, tolerance = 20f))
    }

    @Test
    fun zeroToleranceNeverMatches() {
        assertNull(nearestCodeWithin(100f, 100f, anchors, tolerance = 0f))
    }

    @Test
    fun distanceExactlyAtToleranceCounts() {
        // 10px straight up from RI, tolerance 10 -> inclusive.
        assertEquals("RI", nearestCodeWithin(100f, 90f, anchors, tolerance = 10f))
    }
}
