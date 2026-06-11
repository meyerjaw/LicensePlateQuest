package com.getmecookies.licenseplatequest.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class RecapStatsTest {

    private fun d(s: String) = LocalDate.parse(s)

    @Test
    fun empty_isZero() {
        assertEquals(0, longestConsecutiveDayStreak(emptyList()))
    }

    @Test
    fun singleDay_isOne() {
        assertEquals(1, longestConsecutiveDayStreak(listOf(d("2026-06-05"), d("2026-06-05"))))
    }

    @Test
    fun consecutiveRun_countsContiguousDays() {
        // 4–6 are a 3-run; the 9th is isolated.
        val dates = listOf("2026-06-04", "2026-06-05", "2026-06-06", "2026-06-09").map(::d)
        assertEquals(3, longestConsecutiveDayStreak(dates))
    }

    @Test
    fun ignoresOrderAndDuplicates() {
        val dates = listOf("2026-06-06", "2026-06-04", "2026-06-05", "2026-06-05").map(::d)
        assertEquals(3, longestConsecutiveDayStreak(dates))
    }
}
