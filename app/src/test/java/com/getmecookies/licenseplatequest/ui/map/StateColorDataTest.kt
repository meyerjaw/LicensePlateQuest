package com.getmecookies.licenseplatequest.ui.map

import com.getmecookies.licenseplatequest.domain.STATE_ADJACENCY
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure-JVM guards for the map color data (playtest note #6). These prove the four-color property
 * holds for both the vibrant found mosaic and the subtle unfound base, so a future hand-edit that
 * lets two neighbors share a color fails fast here instead of looking wrong on a device.
 */
class StateColorDataTest {

    @Test
    fun adjacencyIsSymmetric() {
        STATE_ADJACENCY.forEach { (state, neighbors) ->
            neighbors.forEach { n ->
                assertTrue(
                    "Adjacency not symmetric: $state lists $n but not vice-versa",
                    STATE_ADJACENCY[n]?.contains(state) == true,
                )
            }
        }
    }

    @Test
    fun every50StatesCovered() {
        assertEquals(50, STATE_ADJACENCY.size)
        assertTrue(STATE_COLOR_INDEX.keys.containsAll(STATE_ADJACENCY.keys))
        assertTrue(BASE_COLOR_INDEX.keys.containsAll(STATE_ADJACENCY.keys))
    }

    @Test
    fun foundMosaicHasNoAdjacentSameColor() {
        assertNoAdjacentShareColor(STATE_COLOR_INDEX)
    }

    @Test
    fun baseMapIsAValidFourColoring() {
        BASE_COLOR_INDEX.forEach { (state, idx) ->
            assertTrue("Base color index out of 0..3 for $state: $idx", idx in 0..3)
        }
        assertNoAdjacentShareColor(BASE_COLOR_INDEX)
    }

    private fun assertNoAdjacentShareColor(colors: Map<String, Int>) {
        STATE_ADJACENCY.forEach { (state, neighbors) ->
            neighbors.forEach { n ->
                assertTrue(
                    "Neighbors $state and $n share color ${colors[state]}",
                    colors[state] != colors[n],
                )
            }
        }
    }
}
