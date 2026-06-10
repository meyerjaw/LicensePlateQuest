package com.getmecookies.licenseplatequest.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pure-JVM coverage for the achievement catalog. */
class AchievementTest {

    private fun earned(stats: AchievementStats) = evaluateAchievements(stats)

    @Test
    fun emptyStats_earnNothing() {
        assertTrue(earned(AchievementStats()).isEmpty())
    }

    @Test
    fun collectionMilestonesStackByCount() {
        assertEquals(setOf("first_plate"), earned(AchievementStats(lifetimeFound = codes(1))))
        assertTrue(earned(AchievementStats(lifetimeFound = codes(10))).contains("collect_10"))
        val all50 = earned(AchievementStats(lifetimeFound = codes(50)))
        assertTrue(all50.containsAll(setOf("collect_10", "collect_25", "collect_40", "collect_50")))
    }

    @Test
    fun singleTripFeats() {
        assertTrue(earned(AchievementStats(completedTripCount = 1)).contains("first_trip"))
        assertTrue(earned(AchievementStats(reachedFiftyOnATrip = true)).contains("fifty_one_trip"))
        assertTrue(earned(AchievementStats(maxStatesInOneDay = 10)).contains("ten_in_a_day"))
        assertFalse(earned(AchievementStats(maxStatesInOneDay = 9)).contains("ten_in_a_day"))
    }

    @Test
    fun rarity() {
        val rare = setOf("HI", "AK", "ND", "WY", "VT", "SD")
        assertTrue(
            earned(
                AchievementStats(
                    lifetimeFound = setOf("HI"),
                    allRareCodes = rare
                )
            ).contains("rare_catch")
        )
        assertFalse(
            earned(
                AchievementStats(
                    lifetimeFound = setOf("HI"),
                    allRareCodes = rare
                )
            ).contains("treasure_hunter")
        )
        assertTrue(
            earned(
                AchievementStats(
                    lifetimeFound = rare,
                    allRareCodes = rare
                )
            ).contains("treasure_hunter")
        )
    }

    @Test
    fun geographySweeps() {
        assertTrue(earned(AchievementStats(lifetimeFound = NEW_ENGLAND_STATES)).contains("new_england"))
        assertTrue(earned(AchievementStats(lifetimeFound = WEST_COAST_STATES)).contains("west_coast"))
        assertTrue(earned(AchievementStats(lifetimeFound = FOUR_CORNERS_STATES)).contains("four_corners"))
        // A connected run of 5 western states earns "good neighbors".
        assertTrue(
            earned(
                AchievementStats(
                    lifetimeFound = setOf(
                        "CA",
                        "OR",
                        "WA",
                        "NV",
                        "ID"
                    )
                )
            ).contains("good_neighbors")
        )
        // Five *disconnected* states do not.
        assertFalse(
            earned(
                AchievementStats(
                    lifetimeFound = setOf(
                        "CA",
                        "NY",
                        "FL",
                        "TX",
                        "ME"
                    )
                )
            ).contains("good_neighbors")
        )
    }

    @Test
    fun socialAndTime() {
        assertTrue(earned(AchievementStats(maxPlayersOnCompletedTrip = 3)).contains("team_effort"))
        assertTrue(earned(AchievementStats(earliestFindHour = 7)).contains("early_bird"))
        assertFalse(earned(AchievementStats(earliestFindHour = 8)).contains("early_bird"))
    }

    /** A set of [n] distinct dummy codes (sized for count thresholds only). */
    private fun codes(n: Int): Set<String> = (0 until n).map { "X%02d".format(it) }.toSet()
}
