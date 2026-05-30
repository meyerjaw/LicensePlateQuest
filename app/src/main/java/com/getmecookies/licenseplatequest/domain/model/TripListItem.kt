package com.getmecookies.licenseplatequest.domain.model

import java.time.LocalDate
import java.util.UUID

/**
 * A trip as shown on the Trip List (SPEC section 6). [foundCount] of [TOTAL_STATES] drives
 * the progress display; [isComplete] (all states found) flags the special "completed map"
 * styling. Note: per SPEC, reaching 50/50 does NOT change [status] — only a manual end moves
 * a trip to COMPLETED — so completion styling and lifecycle status are independent.
 */
data class TripListItem(
    val id: UUID,
    val name: String,
    val status: TripStatus,
    val startDate: LocalDate,
    val foundCount: Int,
) {
    val isComplete: Boolean get() = foundCount >= TOTAL_STATES

    companion object {
        const val TOTAL_STATES = 50
    }
}
