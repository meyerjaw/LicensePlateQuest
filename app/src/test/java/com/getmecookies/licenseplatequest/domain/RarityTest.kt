package com.getmecookies.licenseplatequest.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pure-JVM coverage for the rare-plate threshold. */
class RarityTest {

    @Test
    fun atOrAboveThresholdIsRare() {
        assertTrue(isRarePlate(RARE_PLATE_THRESHOLD))
        assertTrue(isRarePlate(0.95)) // Hawaii
        assertTrue(isRarePlate(0.6)) // South Dakota, exactly at the threshold
    }

    @Test
    fun belowThresholdIsCommon() {
        assertFalse(isRarePlate(0.59))
        assertFalse(isRarePlate(0.1)) // California
    }
}
