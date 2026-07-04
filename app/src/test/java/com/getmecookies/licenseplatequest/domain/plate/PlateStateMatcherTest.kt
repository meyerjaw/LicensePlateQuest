package com.getmecookies.licenseplatequest.domain.plate

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pure coverage for the OCR-text → state matcher. */
class PlateStateMatcherTest {

    private fun code(vararg lines: String): String? =
        PlateStateMatcher.match(lines.toList())?.stateCode

    @Test
    fun exactStateName_matches() {
        assertEquals("OH", code("OHIO"))
        assertEquals("CA", code("California"))
        assertEquals("TX", code("TEXAS"))
    }

    @Test
    fun multiWordStateName_matches() {
        assertEquals("NY", code("NEW YORK"))
        assertEquals("NC", code("North Carolina"))
        assertEquals("WV", code("WEST VIRGINIA"))
    }

    @Test
    fun nameEmbeddedInALongerLine_matches() {
        // The plate number / county text around the state name shouldn't defeat the match.
        assertEquals("OH", code("OHIO", "ABC 1234"))
        assertEquals("FL", code("XYZ 8890", "THE SUNSHINE STATE"))
    }

    @Test
    fun slogans_matchTheirState() {
        assertEquals("FL", code("SUNSHINE STATE"))
        assertEquals("NY", code("EMPIRE STATE"))
        assertEquals("NJ", code("GARDEN STATE"))
        assertEquals("NH", code("LIVE FREE OR DIE"))
        assertEquals("IL", code("LAND OF LINCOLN"))
    }

    @Test
    fun toleratesOcrNoiseOnLongerNames() {
        // One substituted character on a long name still clears the threshold.
        assertEquals("TN", code("TENNESSEF"))
        assertEquals("PA", code("PENNSYLVANIel".uppercase()))
    }

    @Test
    fun rejectsGarbageAndEmpty() {
        assertNull(code("XQZ 5521"))
        assertNull(code(""))
        assertNull(code("7ABC123", "   "))
    }

    @Test
    fun exactMatchIsFullConfidence() {
        val m = PlateStateMatcher.match(listOf("OREGON"))!!
        assertEquals("OR", m.stateCode)
        assertTrue(m.confidence >= 0.99f)
    }
}
