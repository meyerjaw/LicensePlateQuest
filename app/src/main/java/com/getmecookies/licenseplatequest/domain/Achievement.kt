package com.getmecookies.licenseplatequest.domain

/**
 * A snapshot of the player's progress, used to evaluate achievements. Framework-free so the catalog
 * can be unit-tested on the plain JVM; the repository builds this from the database.
 */
data class AchievementStats(
    val lifetimeFound: Set<String> = emptySet(),
    val allRareCodes: Set<String> = emptySet(),
    val completedTripCount: Int = 0,
    val reachedFiftyOnATrip: Boolean = false,
    val maxStatesInOneDay: Int = 0,
    val maxPlayersOnCompletedTrip: Int = 0,
    /** Earliest local hour-of-day (0–23) across all finds, or null if none. */
    val earliestFindHour: Int? = null,
) {
    /** Rare states the player has actually collected. */
    val rareFound: Set<String> get() = lifetimeFound intersect allRareCodes
}

/**
 * The achievement catalog. Each has a stable [id] (persisted to the DB) and a pure [check] over an
 * [AchievementStats] snapshot. Titles, descriptions, and icons live in the UI layer so this stays
 * Android-free and fully unit-testable.
 */
enum class Achievement(val id: String, val check: (AchievementStats) -> Boolean) {
    // Collection milestones (lifetime, across all trips).
    FIRST_PLATE("first_plate", { it.lifetimeFound.isNotEmpty() }),
    COLLECT_10("collect_10", { it.lifetimeFound.size >= 10 }),
    COLLECT_25("collect_25", { it.lifetimeFound.size >= 25 }),
    COLLECT_40("collect_40", { it.lifetimeFound.size >= 40 }),
    COLLECT_50("collect_50", { it.lifetimeFound.size >= 50 }),

    // Single-trip feats.
    FIRST_TRIP("first_trip", { it.completedTripCount >= 1 }),
    FIFTY_ON_ONE_TRIP("fifty_one_trip", { it.reachedFiftyOnATrip }),
    TEN_IN_A_DAY("ten_in_a_day", { it.maxStatesInOneDay >= 10 }),

    // Rarity.
    RARE_CATCH("rare_catch", { it.rareFound.isNotEmpty() }),
    TREASURE_HUNTER(
        "treasure_hunter",
        { it.allRareCodes.isNotEmpty() && it.lifetimeFound.containsAll(it.allRareCodes) },
    ),

    // Geography sweeps.
    NEW_ENGLAND("new_england", { it.lifetimeFound.containsAll(NEW_ENGLAND_STATES) }),
    WEST_COAST("west_coast", { it.lifetimeFound.containsAll(WEST_COAST_STATES) }),
    FOUR_CORNERS("four_corners", { it.lifetimeFound.containsAll(FOUR_CORNERS_STATES) }),
    GOOD_NEIGHBORS("good_neighbors", { largestConnectedCluster(it.lifetimeFound) >= 5 }),

    // Social / time flavor.
    TEAM_EFFORT("team_effort", { it.maxPlayersOnCompletedTrip >= 3 }),
    EARLY_BIRD("early_bird", { it.earliestFindHour != null && it.earliestFindHour < 8 }),
    ;

    companion object {
        fun byId(id: String): Achievement? = entries.firstOrNull { it.id == id }
    }
}

/** The ids of all achievements earned for the given [stats]. */
fun evaluateAchievements(stats: AchievementStats): Set<String> =
    Achievement.entries.filter { it.check(stats) }.map { it.id }.toSet()
