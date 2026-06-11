package com.getmecookies.licenseplatequest.domain

import kotlin.math.min

/**
 * A snapshot of the player's progress, used to evaluate achievements. Framework-free so the catalog
 * can be unit-tested on the plain JVM; the repository builds this from the database.
 */
data class AchievementStats(
    val lifetimeFound: Set<String> = emptySet(),
    val allRareCodes: Set<String> = emptySet(),
    val completedTripCount: Int = 0,
    val reachedFiftyOnATrip: Boolean = false,
    /** Most states ever collected on a single trip (drives the 50/50 progress bar). */
    val maxStatesOnATrip: Int = 0,
    val maxStatesInOneDay: Int = 0,
    val maxPlayersOnCompletedTrip: Int = 0,
    /** Earliest local hour-of-day (0–23) across all finds, or null if none. */
    val earliestFindHour: Int? = null,
    /** Latest local hour-of-day (0–23) across all finds, or null if none. */
    val latestFindHour: Int? = null,
    /** Whether any find happened on a Saturday / Sunday (local time). */
    val foundOnSaturday: Boolean = false,
    val foundOnSunday: Boolean = false,
) {
    /** Rare states the player has actually collected. */
    val rareFound: Set<String> get() = lifetimeFound intersect allRareCodes
}

/**
 * How close the player is to an achievement: [current] of [target] steps done. A [target] of 1 is a
 * simple yes/no badge (no meaningful bar); a larger target is shown as a "3 / 6" progress bar on the
 * Passport. Always clamped so 0 ≤ current ≤ target.
 */
data class AchievementProgress(val current: Int, val target: Int) {
    val isComplete: Boolean get() = current >= target
    val isBinary: Boolean get() = target <= 1
    val fraction: Float
        get() = if (target <= 0) 0f else (current.toFloat() / target).coerceIn(
            0f,
            1f
        )
}

/**
 * The achievement catalog. Each has a stable [id] (persisted to the DB) and a pure [progress]
 * function over an [AchievementStats] snapshot. An achievement is earned once its progress
 * [AchievementProgress.isComplete]. Titles, descriptions, and icons live in the UI layer so this
 * stays Android-free and fully unit-testable.
 */
enum class Achievement(val id: String, val progress: (AchievementStats) -> AchievementProgress) {
    // Collection milestones (lifetime, across all trips).
    FIRST_PLATE("first_plate", { binary(it.lifetimeFound.isNotEmpty()) }),
    COLLECT_10("collect_10", { tally(it.lifetimeFound.size, 10) }),
    COLLECT_25("collect_25", { tally(it.lifetimeFound.size, 25) }),
    COLLECT_40("collect_40", { tally(it.lifetimeFound.size, 40) }),
    COLLECT_50("collect_50", { tally(it.lifetimeFound.size, 50) }),

    // Single-trip feats.
    FIRST_TRIP("first_trip", { tally(it.completedTripCount, 1) }),
    FIFTY_ON_ONE_TRIP("fifty_one_trip", {
        // Honor the legacy boolean so older snapshots still earn it; otherwise track the best trip.
        tally(if (it.reachedFiftyOnATrip) 50 else it.maxStatesOnATrip, 50)
    }),
    TEN_IN_A_DAY("ten_in_a_day", { tally(it.maxStatesInOneDay, 10) }),

    // Rarity.
    RARE_CATCH("rare_catch", { binary(it.rareFound.isNotEmpty()) }),
    TREASURE_HUNTER(
        "treasure_hunter",
        {
            if (it.allRareCodes.isEmpty()) AchievementProgress(0, 1)
            else sweep(it.lifetimeFound, it.allRareCodes)
        },
    ),

    // Geography sweeps.
    NEW_ENGLAND("new_england", { sweep(it.lifetimeFound, NEW_ENGLAND_STATES) }),
    WEST_COAST("west_coast", { sweep(it.lifetimeFound, WEST_COAST_STATES) }),
    FOUR_CORNERS("four_corners", { sweep(it.lifetimeFound, FOUR_CORNERS_STATES) }),
    GREAT_LAKES("great_lakes", { sweep(it.lifetimeFound, GREAT_LAKES_STATES) }),
    DEEP_SOUTH("deep_south", { sweep(it.lifetimeFound, DEEP_SOUTH_STATES) }),
    MOUNTAIN_WEST("mountain_west", { sweep(it.lifetimeFound, MOUNTAIN_WEST_STATES) }),
    GOOD_NEIGHBORS("good_neighbors", { tally(largestConnectedCluster(it.lifetimeFound), 5) }),
    COAST_TO_COAST("coast_to_coast", {
        val pacific = if ((it.lifetimeFound intersect PACIFIC_COAST_STATES).isNotEmpty()) 1 else 0
        val atlantic = if ((it.lifetimeFound intersect ATLANTIC_COAST_STATES).isNotEmpty()) 1 else 0
        AchievementProgress(pacific + atlantic, 2)
    }),

    // Social / time flavor.
    TEAM_EFFORT("team_effort", { tally(it.maxPlayersOnCompletedTrip, 3) }),
    EARLY_BIRD("early_bird", { binary(it.earliestFindHour != null && it.earliestFindHour < 8) }),
    NIGHT_OWL("night_owl", { binary(it.latestFindHour != null && it.latestFindHour >= 21) }),
    WEEKEND_WARRIOR("weekend_warrior", {
        AchievementProgress(
            (if (it.foundOnSaturday) 1 else 0) + (if (it.foundOnSunday) 1 else 0),
            2
        )
    }),
    ;

    /** Whether this achievement is earned for the given [stats]. */
    fun isEarned(stats: AchievementStats): Boolean = progress(stats).isComplete

    companion object {
        fun byId(id: String): Achievement? = entries.firstOrNull { it.id == id }
    }
}

/** A yes/no achievement: complete when [value] is true. */
private fun binary(value: Boolean): AchievementProgress =
    AchievementProgress(if (value) 1 else 0, 1)

/** A counting achievement: [current] toward [target], clamped so the bar never overflows. */
private fun tally(current: Int, target: Int): AchievementProgress =
    AchievementProgress(min(current.coerceAtLeast(0), target), target)

/** A "collect every member of this set" achievement. */
private fun sweep(found: Set<String>, set: Set<String>): AchievementProgress =
    AchievementProgress((found intersect set).size, set.size)

/** The ids of all achievements earned for the given [stats]. */
fun evaluateAchievements(stats: AchievementStats): Set<String> =
    Achievement.entries.filter { it.isEarned(stats) }.map { it.id }.toSet()
