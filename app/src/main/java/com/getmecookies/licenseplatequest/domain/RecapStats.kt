package com.getmecookies.licenseplatequest.domain

import java.time.LocalDate

/**
 * The longest run of **consecutive calendar days** that each have at least one entry in [dates] —
 * the trip's best "streak" (richer recap). Duplicate dates count once; order doesn't matter.
 * Returns 0 for no dates, 1 when no two days are adjacent.
 */
fun longestConsecutiveDayStreak(dates: Collection<LocalDate>): Int {
    val sorted = dates.toSortedSet()
    if (sorted.isEmpty()) return 0
    var best = 1
    var current = 1
    var prev: LocalDate? = null
    for (day in sorted) {
        val p = prev
        if (p != null) {
            current = if (day == p.plusDays(1)) current + 1 else 1
            if (current > best) best = current
        }
        prev = day
    }
    return best
}
