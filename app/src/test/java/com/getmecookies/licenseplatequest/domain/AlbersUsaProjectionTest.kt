package com.getmecookies.licenseplatequest.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Golden-value tests for the Albers-USA projection. Expected x/y were produced by an offline
 * reference implementation and cross-checked: each city projects inside the correct state polygon
 * of the bundled map (see the project's tools). Tolerance covers the reference's 0.1 rounding.
 */
class AlbersUsaProjectionTest {

    private fun assertPoint(lat: Double, lng: Double, x: Float, y: Float) {
        val p = AlbersUsaProjection.project(lat, lng)
            ?: error("expected a projected point for ($lat, $lng)")
        assertEquals(x.toDouble(), p.x.toDouble(), 0.6)
        assertEquals(y.toDouble(), p.y.toDouble(), 0.6)
    }

    @Test
    fun lower48Cities() {
        assertPoint(39.96, -82.99, 787.2f, 255.9f) // Columbus, OH
        assertPoint(38.25, -85.76, 744.0f, 300.6f) // Louisville, KY
        assertPoint(34.05, -118.24, 152.5f, 358.2f) // Los Angeles, CA
        assertPoint(40.71, -74.01, 935.3f, 210.8f) // New York, NY
        assertPoint(47.61, -122.33, 163.3f, 41.3f) // Seattle, WA
    }

    @Test
    fun alaskaAndHawaiiInsets() {
        assertPoint(61.22, -149.90, 177.9f, 539.3f) // Anchorage, AK
        assertPoint(21.31, -157.86, 332.6f, 544.1f) // Honolulu, HI
    }

    @Test
    fun offMapReturnsNull() {
        assertNull(AlbersUsaProjection.project(0.0, 0.0)) // Gulf of Guinea
        assertNull(AlbersUsaProjection.project(51.5, -0.12)) // London
    }

    @Test
    fun pointsLandInMapBounds() {
        // Bundled map viewBox is 1030.7 x 609.6; projected US points stay within it.
        val p = AlbersUsaProjection.project(39.96, -82.99)!!
        assertTrue(p.x in 0f..1030.7f && p.y in 0f..609.6f)
    }
}
