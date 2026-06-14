package com.getmecookies.licenseplatequest.domain

import com.getmecookies.licenseplatequest.domain.model.ThemeMode

/**
 * The non-PII analytics user-property cohorts — coarse buckets so every event can be segmented
 * (e.g. retention of solo vs. group players) without identifying anyone. Pure bucketing keeps it
 * fully unit-testable; [apply] pushes a snapshot to an [Analytics]. Counts are bucketed (never raw)
 * to avoid singling out unusual users.
 */
object UserProperties {
    const val PLAYER_COUNT_BUCKET = "player_count_bucket"
    const val HAS_COMPLETED_TRIP = "has_completed_trip"
    const val LIFETIME_STATES_BUCKET = "lifetime_states_bucket"
    const val THEME_PREF = "theme_pref"

    /** Roster size → bucket. */
    fun playerCountBucket(count: Int): String = when {
        count <= 0 -> "0"
        count == 1 -> "1"
        count == 2 -> "2"
        count <= 4 -> "3-4"
        else -> "5+"
    }

    /** Lifetime distinct states found → bucket (50 is its own bucket — the whole-set milestone). */
    fun lifetimeStatesBucket(count: Int): String = when {
        count <= 0 -> "0"
        count <= 10 -> "1-10"
        count <= 25 -> "11-25"
        count < 50 -> "26-49"
        else -> "50"
    }

    /** The full cohort snapshot keyed by property name. */
    fun snapshot(
        playerCount: Int,
        hasCompletedTrip: Boolean,
        lifetimeStatesFound: Int,
        theme: ThemeMode,
    ): Map<String, String> = mapOf(
        PLAYER_COUNT_BUCKET to playerCountBucket(playerCount),
        HAS_COMPLETED_TRIP to hasCompletedTrip.toString(),
        LIFETIME_STATES_BUCKET to lifetimeStatesBucket(lifetimeStatesFound),
        THEME_PREF to theme.name.lowercase(),
    )

    /** Compute the snapshot and push each property to [analytics]. */
    fun apply(
        analytics: Analytics,
        playerCount: Int,
        hasCompletedTrip: Boolean,
        lifetimeStatesFound: Int,
        theme: ThemeMode,
    ) {
        snapshot(playerCount, hasCompletedTrip, lifetimeStatesFound, theme)
            .forEach { (name, value) -> analytics.setUserProperty(name, value) }
    }
}
