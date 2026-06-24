package com.getmecookies.licenseplatequest.domain

import com.getmecookies.licenseplatequest.domain.model.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Test

/** Pure coverage for the analytics user-property cohorts: bucketing edges + the applied snapshot. */
class UserPropertiesTest {

    @Test
    fun playerCountBucket_coversEdges() {
        assertEquals("0", UserProperties.playerCountBucket(0))
        assertEquals("1", UserProperties.playerCountBucket(1))
        assertEquals("2", UserProperties.playerCountBucket(2))
        assertEquals("3-4", UserProperties.playerCountBucket(3))
        assertEquals("3-4", UserProperties.playerCountBucket(4))
        assertEquals("5+", UserProperties.playerCountBucket(5))
        assertEquals("5+", UserProperties.playerCountBucket(12))
    }

    @Test
    fun lifetimeStatesBucket_coversEdges() {
        assertEquals("0", UserProperties.lifetimeStatesBucket(0))
        assertEquals("1-10", UserProperties.lifetimeStatesBucket(1))
        assertEquals("1-10", UserProperties.lifetimeStatesBucket(10))
        assertEquals("11-25", UserProperties.lifetimeStatesBucket(11))
        assertEquals("11-25", UserProperties.lifetimeStatesBucket(25))
        assertEquals("26-49", UserProperties.lifetimeStatesBucket(26))
        assertEquals("26-49", UserProperties.lifetimeStatesBucket(49))
        assertEquals("50", UserProperties.lifetimeStatesBucket(50))
    }

    @Test
    fun snapshot_buildsAllFourCohorts() {
        val snapshot = UserProperties.snapshot(
            playerCount = 3,
            hasCompletedTrip = true,
            lifetimeStatesFound = 27,
            theme = ThemeMode.DARK,
        )
        assertEquals("3-4", snapshot[UserProperties.PLAYER_COUNT_BUCKET])
        assertEquals("true", snapshot[UserProperties.HAS_COMPLETED_TRIP])
        assertEquals("26-49", snapshot[UserProperties.LIFETIME_STATES_BUCKET])
        assertEquals("dark", snapshot[UserProperties.THEME_PREF])
    }

    @Test
    fun apply_pushesEachPropertyToAnalytics() {
        val analytics = FakeAnalytics()

        UserProperties.apply(
            analytics = analytics,
            playerCount = 0,
            hasCompletedTrip = false,
            lifetimeStatesFound = 50,
            theme = ThemeMode.SYSTEM,
        )

        assertEquals("0", analytics.userProperties[UserProperties.PLAYER_COUNT_BUCKET])
        assertEquals("false", analytics.userProperties[UserProperties.HAS_COMPLETED_TRIP])
        assertEquals("50", analytics.userProperties[UserProperties.LIFETIME_STATES_BUCKET])
        assertEquals("system", analytics.userProperties[UserProperties.THEME_PREF])
    }
}
