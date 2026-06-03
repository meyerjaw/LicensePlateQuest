package com.getmecookies.licenseplatequest.domain.model

import java.util.UUID

/**
 * One player's standing on the celebration leaderboard (playtest note #18): how many plates they
 * were credited for, and whether they're (tied for) the lead — which earns the crown.
 */
data class PlayerScore(
    val id: UUID,
    val name: String,
    val colorToken: String?,
    val score: Int,
    val isLeader: Boolean,
)

/**
 * Computed stats shown on the celebration screens (SPEC section 6). All values are
 * pre-formatted for display; fields that can't be computed (e.g. gaps with <2 finds) are
 * null and the screen omits their rows.
 */
data class CelebrationStats(
    val tripName: String,
    val foundCount: Int,
    /** Region codes found on this trip, for the filled summary map (playtest note #3). */
    val foundCodes: Set<String>,
    /** Per-player credit standings, highest first (playtest note #18). */
    val leaderboard: List<PlayerScore>,
    /** Plates with no credited player, shown as a separate leaderboard line. */
    val unattributedCount: Int,
    val durationText: String,
    val averageGapText: String?,
    val longestGapText: String?,
    val shortestGapText: String?,
    val firstStateName: String?,
    val lastStateName: String?,
    val estimatedDistanceText: String?,
    val furthestStateName: String?,
    val rarestStateName: String?,
    val playerNames: List<String>,
)
