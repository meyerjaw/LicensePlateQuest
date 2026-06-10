package com.getmecookies.licenseplatequest.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pure-JVM coverage for the geography helpers. */
class StateGeographyTest {

    @Test
    fun emptyAndSingle() {
        assertEquals(0, largestConnectedCluster(emptySet()))
        assertEquals(1, largestConnectedCluster(setOf("CA")))
    }

    @Test
    fun reportsLargestConnectedRun() {
        // CA-OR-WA-NV-ID are all connected -> 5; the lone FL is its own component.
        assertEquals(5, largestConnectedCluster(setOf("CA", "OR", "WA", "NV", "ID", "FL")))
    }

    @Test
    fun disconnectedStatesStayInTheirOwnClusters() {
        // No two of these border each other.
        assertEquals(1, largestConnectedCluster(setOf("CA", "NY", "FL", "TX", "ME")))
    }

    @Test
    fun adjacencyIsSymmetricAndRegionsAreValid() {
        STATE_ADJACENCY.forEach { (state, neighbors) ->
            neighbors.forEach { n ->
                assertTrue("$state-$n asymmetric", STATE_ADJACENCY[n]?.contains(state) == true)
            }
        }
        (NEW_ENGLAND_STATES + WEST_COAST_STATES + FOUR_CORNERS_STATES).forEach {
            assertTrue("$it not a known state", STATE_ADJACENCY.containsKey(it))
        }
    }
}
