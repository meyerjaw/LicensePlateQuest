package com.getmecookies.licenseplatequest.domain.model

import java.time.Instant
import java.util.UUID

/** One stop on the trip's journey timeline (a found state, in the order it was caught). */
data class TimelineFind(
    val code: String,
    val name: String,
    val foundAt: Instant,
)

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
 * A per-player recap highlight (richer recap): how many plates they were credited for and their
 * standout (rarest) catch. Only players with at least one credited find get a highlight.
 */
data class PlayerHighlight(
    val id: UUID,
    val name: String,
    val colorToken: String?,
    val count: Int,
    val rarestStateName: String?,
    /** Whether [rarestStateName] is an actual rare plate (drives the ✦ flourish). */
    val rarestIsRare: Boolean,
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
    /** The single day with the most finds, pre-formatted (e.g. "8 states · Jun 5"), or null. */
    val busiestDayText: String? = null,
    /** Longest run of consecutive days with a find, pre-formatted (e.g. "4-day streak"), or null. */
    val longestStreakText: String? = null,
    /** Per-player highlights (credited count + rarest catch), highest count first (richer recap). */
    val playerHighlights: List<PlayerHighlight> = emptyList(),
    /** The finds in the order they were caught — the trip's journey (richer recap). */
    val timeline: List<TimelineFind> = emptyList(),
    val playerNames: List<String>,
)
