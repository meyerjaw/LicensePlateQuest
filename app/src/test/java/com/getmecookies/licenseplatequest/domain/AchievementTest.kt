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

    @Test
    fun newGeographySweeps() {
        assertTrue(earned(AchievementStats(lifetimeFound = GREAT_LAKES_STATES)).contains("great_lakes"))
        assertTrue(earned(AchievementStats(lifetimeFound = DEEP_SOUTH_STATES)).contains("deep_south"))
        assertTrue(earned(AchievementStats(lifetimeFound = MOUNTAIN_WEST_STATES)).contains("mountain_west"))
        // A partial set does not earn the sweep.
        assertFalse(
            earned(
                AchievementStats(
                    lifetimeFound = setOf(
                        "AL",
                        "GA"
                    )
                )
            ).contains("deep_south")
        )
    }

    @Test
    fun coastToCoast_needsBothCoasts() {
        assertFalse(
            earned(
                AchievementStats(
                    lifetimeFound = setOf(
                        "CA",
                        "OR"
                    )
                )
            ).contains("coast_to_coast")
        )
        assertFalse(
            earned(
                AchievementStats(
                    lifetimeFound = setOf(
                        "ME",
                        "FL"
                    )
                )
            ).contains("coast_to_coast")
        )
        assertTrue(
            earned(
                AchievementStats(
                    lifetimeFound = setOf(
                        "CA",
                        "FL"
                    )
                )
            ).contains("coast_to_coast")
        )
    }

    @Test
    fun nightOwlAndWeekendWarrior() {
        assertTrue(earned(AchievementStats(latestFindHour = 21)).contains("night_owl"))
        assertFalse(earned(AchievementStats(latestFindHour = 20)).contains("night_owl"))
        assertFalse(earned(AchievementStats(foundOnSaturday = true)).contains("weekend_warrior"))
        assertTrue(
            earned(AchievementStats(foundOnSaturday = true, foundOnSunday = true))
                .contains("weekend_warrior"),
        )
    }

    @Test
    fun fiftyOnOneTrip_tracksBestTrip() {
        assertFalse(earned(AchievementStats(maxStatesOnATrip = 49)).contains("fifty_one_trip"))
        assertTrue(earned(AchievementStats(maxStatesOnATrip = 50)).contains("fifty_one_trip"))
        // The legacy boolean still earns it.
        assertTrue(earned(AchievementStats(reachedFiftyOnATrip = true)).contains("fifty_one_trip"))
    }

    @Test
    fun progressReportsPartialAndClampedValues() {
        val p = Achievement.COLLECT_25.progress(AchievementStats(lifetimeFound = codes(10)))
        assertEquals(10, p.current)
        assertEquals(25, p.target)
        assertFalse(p.isComplete)
        // Counting progress never overflows its target.
        val over = Achievement.COLLECT_10.progress(AchievementStats(lifetimeFound = codes(50)))
        assertEquals(10, over.current)
        assertTrue(over.isComplete)
        // A binary badge reports a target of 1.
        val binary = Achievement.FIRST_PLATE.progress(AchievementStats(lifetimeFound = codes(1)))
        assertTrue(binary.isBinary)
        assertTrue(binary.isComplete)
    }

    /** A set of [n] distinct dummy codes (sized for count thresholds only). */
    private fun codes(n: Int): Set<String> = (0 until n).map { "X%02d".format(it) }.toSet()
}
