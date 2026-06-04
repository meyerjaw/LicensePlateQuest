package com.getmecookies.licenseplatequest.domain.model

import com.getmecookies.licenseplatequest.domain.model.TripListItem.Companion.TOTAL_STATES
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.UUID

/**
 * A trip as shown on the Trip List (SPEC section 6). [foundCount] of [TOTAL_STATES] drives
 * the progress display; [isComplete] (all states found) flags the special "completed map"
 * styling. Note: per SPEC, reaching 50/50 does NOT change [status] — only a manual end moves
 * a trip to COMPLETED — so completion styling and lifecycle status are independent.
 *
 * [endedAt] is set only when the trip was manually ended; [createdAt] is when it was started.
 * Together they give the trip's duration for completed rows.
 */
data class TripListItem(
    val id: UUID,
    val name: String,
    val status: TripStatus,
    val startDate: LocalDate,
    val endDate: LocalDate?,
    val endedAt: Instant?,
    val createdAt: Instant,
    val foundCount: Int,
) {
    val isComplete: Boolean get() = foundCount >= TOTAL_STATES

    /** Past its end date and not yet ended — shows an "Overdue" hint (playtest notes #12/#13). */
    val isOverdue: Boolean
        get() = endDate != null &&
            status != TripStatus.COMPLETED &&
            endDate.isBefore(LocalDate.now())

    /**
     * Human-readable trip length for completed trips (createdAt → endedAt), or null when the
     * trip isn't ended yet. Rounds to whole days, then hours, then minutes.
     */
    val durationLabel: String?
        get() {
            val ended = endedAt ?: return null
            val minutes = ChronoUnit.MINUTES.between(createdAt, ended).coerceAtLeast(0)
            return when {
                minutes >= 1440 -> {
                    val days = minutes / 1440
                    "$days ${plural(days, "day")}"
                }
                minutes >= 60 -> {
                    val hours = minutes / 60
                    "$hours ${plural(hours, "hour")}"
                }
                else -> "$minutes ${plural(minutes, "minute")}"
            }
        }

    private fun plural(n: Long, unit: String): String = if (n == 1L) unit else "${unit}s"

    companion object {
        const val TOTAL_STATES = 50
    }
}
