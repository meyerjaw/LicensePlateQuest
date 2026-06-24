package com.getmecookies.licenseplatequest.data.analytics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Coverage for [analyticsParamsToBundle] — the seam-params → Firebase Bundle mapping. Robolectric
 * supplies a real [android.os.Bundle]. The Firebase SDK itself isn't exercised here.
 */
@RunWith(RobolectricTestRunner::class)
class FirebaseAnalyticsClientTest {

    @Test
    fun mapsSupportedTypesToBundle() {
        val bundle = analyticsParamsToBundle(
            mapOf(
                "s" to "hi",
                "i" to 7,
                "l" to 9L,
                "d" to 1.5,
                "f" to 2.5f,
                "b" to true,
            ),
        )

        assertEquals("hi", bundle.getString("s"))
        assertEquals(7L, bundle.getLong("i")) // Int widened to Long
        assertEquals(9L, bundle.getLong("l"))
        assertEquals(1.5, bundle.getDouble("d"), 0.0)
        assertEquals(2.5, bundle.getDouble("f"), 0.0) // Float widened to Double
        assertEquals("true", bundle.getString("b")) // Boolean → readable string
    }

    @Test
    fun dropsNullValues() {
        val bundle = analyticsParamsToBundle(mapOf("present" to "x", "absent" to null))

        assertEquals("x", bundle.getString("present"))
        assertFalse(bundle.containsKey("absent"))
    }

    @Test
    fun fallsBackToStringForOtherTypes() {
        val bundle = analyticsParamsToBundle(mapOf("list" to listOf(1, 2)))

        assertEquals(listOf(1, 2).toString(), bundle.getString("list"))
    }
}
