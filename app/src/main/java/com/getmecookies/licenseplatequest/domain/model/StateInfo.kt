package com.getmecookies.licenseplatequest.domain.model

import java.time.Instant
import java.util.UUID

/**
 * Bundled, static facts about a state, shown on the State Detail screen (SPEC section 6).
 * Sourced from the seeded PlateRegion table.
 */
data class StateInfo(
    val code: String,
    val name: String,
    val bird: String,
    val motto: String,
    val flower: String,
    val funFacts: List<String>,
    /** Bundled rarity score (0..1); high values are "rare finds" (rare-plate moments). */
    val rarityScore: Double = 0.0,
)

/**
 * Everything State Detail needs to render: the static [info] plus the player's progress on
 * the active trip. When [found] is true, [foundAt]/[foundTripName] describe when and where
 * it was spotted (SPEC: "Found timestamp, trip name where it was found").
 *
 * [hasActiveTrip] is false when no trip is active — marking is then disabled, since a
 * spotting must belong to a trip's game.
 */
data class StateDetailData(
    val info: StateInfo,
    val hasActiveTrip: Boolean,
    val found: Boolean,
    val foundAt: Instant? = null,
    val foundTripName: String? = null,
    /** The active trip's players, for the attribution multi-select (playtest note #17). */
    val tripPlayers: List<Player> = emptyList(),
    /**
     * Initial attribution selection: the players already credited (when [found]), or the
     * remembered/auto selection to pre-check when about to mark.
     */
    val initialAttribution: Set<UUID> = emptySet(),
)
