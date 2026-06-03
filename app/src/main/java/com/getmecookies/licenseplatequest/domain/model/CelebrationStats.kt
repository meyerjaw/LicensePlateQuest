package com.getmecookies.licenseplatequest.domain.model

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
