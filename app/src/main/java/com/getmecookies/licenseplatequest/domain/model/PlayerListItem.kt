package com.getmecookies.licenseplatequest.domain.model

import java.time.LocalDate

/**
 * A player plus the trip-based play stats shown on each Players list row.
 *
 * [tripCount] — total trips joined ("total plays" in the UI).
 * [lastPlayed] — start date of the most recent trip joined, or null if never played.
 */
data class PlayerListItem(
    val player: Player,
    val tripCount: Int,
    val lastPlayed: LocalDate?,
)
