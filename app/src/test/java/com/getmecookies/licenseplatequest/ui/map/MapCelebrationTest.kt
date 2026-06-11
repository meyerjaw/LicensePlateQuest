package com.getmecookies.licenseplatequest.ui.map

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MapCelebrationTest {

    @Test
    fun singleFind_sweepsAloneNoCombo() {
        val t = celebrationTiming(1)
        assertEquals(0, t.staggerMs)
        assertFalse(t.combo)
        assertEquals(t.fillMs, t.totalMs(1))
    }

    @Test
    fun smallBatch_staggersWithoutCombo() {
        val t = celebrationTiming(3)
        assertTrue(t.staggerMs > 0)
        assertFalse(t.combo)
        // Last of 3 starts at 2·stagger, then its own fill.
        assertEquals(2 * t.staggerMs + t.fillMs, t.totalMs(3))
    }

    @Test
    fun largeBatch_isComboWithHold() {
        val t = celebrationTiming(6)
        assertTrue(t.combo)
        assertTrue("combo should be quicker per-state", t.fillMs < celebrationTiming(3).fillMs)
        // The overlay lingers after the last fill (hold), so total includes it.
        assertTrue("combo should hold the overlay", t.holdMs > 0)
        assertEquals(5 * t.staggerMs + t.fillMs + t.holdMs, t.totalMs(6))
    }
}
