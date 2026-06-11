package com.getmecookies.licenseplatequest.domain

/**
 * Pure (Android-free) US geography data: land-border adjacency, a few named regions, and a helper
 * to size the largest connected cluster of states. Used by achievements (geography sweeps + "good
 * neighbors") and the map color tests. Unit-testable on the plain JVM.
 */

/** Land-border adjacency between the 50 states (symmetric). AK and HI have no neighbors. */
val STATE_ADJACENCY: Map<String, Set<String>> = mapOf(
    "AK" to setOf(),
    "AL" to setOf("FL", "GA", "MS", "TN"),
    "AR" to setOf("LA", "MO", "MS", "OK", "TN", "TX"),
    "AZ" to setOf("CA", "CO", "NM", "NV", "UT"),
    "CA" to setOf("AZ", "NV", "OR"),
    "CO" to setOf("AZ", "KS", "NE", "NM", "OK", "UT", "WY"),
    "CT" to setOf("MA", "NY", "RI"),
    "DE" to setOf("MD", "NJ", "PA"),
    "FL" to setOf("AL", "GA"),
    "GA" to setOf("AL", "FL", "NC", "SC", "TN"),
    "HI" to setOf(),
    "IA" to setOf("IL", "MN", "MO", "NE", "SD", "WI"),
    "ID" to setOf("MT", "NV", "OR", "UT", "WA", "WY"),
    "IL" to setOf("IA", "IN", "KY", "MO", "WI"),
    "IN" to setOf("IL", "KY", "MI", "OH"),
    "KS" to setOf("CO", "MO", "NE", "OK"),
    "KY" to setOf("IL", "IN", "MO", "OH", "TN", "VA", "WV"),
    "LA" to setOf("AR", "MS", "TX"),
    "MA" to setOf("CT", "NH", "NY", "RI", "VT"),
    "MD" to setOf("DE", "PA", "VA", "WV"),
    "ME" to setOf("NH"),
    "MI" to setOf("IN", "OH", "WI"),
    "MN" to setOf("IA", "ND", "SD", "WI"),
    "MO" to setOf("AR", "IA", "IL", "KS", "KY", "NE", "OK", "TN"),
    "MS" to setOf("AL", "AR", "LA", "TN"),
    "MT" to setOf("ID", "ND", "SD", "WY"),
    "NC" to setOf("GA", "SC", "TN", "VA"),
    "ND" to setOf("MN", "MT", "SD"),
    "NE" to setOf("CO", "IA", "KS", "MO", "SD", "WY"),
    "NH" to setOf("MA", "ME", "VT"),
    "NJ" to setOf("DE", "NY", "PA"),
    "NM" to setOf("AZ", "CO", "OK", "TX", "UT"),
    "NV" to setOf("AZ", "CA", "ID", "OR", "UT"),
    "NY" to setOf("CT", "MA", "NJ", "PA", "VT"),
    "OH" to setOf("IN", "KY", "MI", "PA", "WV"),
    "OK" to setOf("AR", "CO", "KS", "MO", "NM", "TX"),
    "OR" to setOf("CA", "ID", "NV", "WA"),
    "PA" to setOf("DE", "MD", "NJ", "NY", "OH", "WV"),
    "RI" to setOf("CT", "MA"),
    "SC" to setOf("GA", "NC"),
    "SD" to setOf("IA", "MN", "MT", "ND", "NE", "WY"),
    "TN" to setOf("AL", "AR", "GA", "KY", "MO", "MS", "NC", "VA"),
    "TX" to setOf("AR", "LA", "NM", "OK"),
    "UT" to setOf("AZ", "CO", "ID", "NM", "NV", "WY"),
    "VA" to setOf("KY", "MD", "NC", "TN", "WV"),
    "VT" to setOf("MA", "NH", "NY"),
    "WA" to setOf("ID", "OR"),
    "WI" to setOf("IA", "IL", "MI", "MN"),
    "WV" to setOf("KY", "MD", "OH", "PA", "VA"),
    "WY" to setOf("CO", "ID", "MT", "NE", "SD", "UT"),
)

/** Named state groups for the geography-sweep achievements. */
val NEW_ENGLAND_STATES = setOf("CT", "ME", "MA", "NH", "RI", "VT")
val WEST_COAST_STATES = setOf("CA", "OR", "WA")
val FOUR_CORNERS_STATES = setOf("AZ", "CO", "NM", "UT")

/** The eight states bordering the Great Lakes. */
val GREAT_LAKES_STATES = setOf("MN", "WI", "IL", "IN", "MI", "OH", "PA", "NY")

/** The Deep South. */
val DEEP_SOUTH_STATES = setOf("AL", "GA", "LA", "MS", "SC")

/** The Mountain West (Census Mountain division). */
val MOUNTAIN_WEST_STATES = setOf("MT", "ID", "WY", "NV", "UT", "CO", "AZ", "NM")

/** Pacific-coast states (incl. Alaska & Hawaii) — for the coast-to-coast achievement. */
val PACIFIC_COAST_STATES = setOf("CA", "OR", "WA", "AK", "HI")

/** Atlantic-seaboard states — for the coast-to-coast achievement. */
val ATLANTIC_COAST_STATES =
    setOf("ME", "NH", "MA", "RI", "CT", "NY", "NJ", "DE", "MD", "VA", "NC", "SC", "GA", "FL")

/**
 * The size of the largest cluster of [found] states that are connected through [adjacency]
 * (border-to-border). Used by the "good neighbors" achievement. Pure BFS over the found subgraph.
 */
fun largestConnectedCluster(
    found: Set<String>,
    adjacency: Map<String, Set<String>> = STATE_ADJACENCY,
): Int {
    val unvisited = found.toMutableSet()
    var largest = 0
    while (unvisited.isNotEmpty()) {
        val start = unvisited.first()
        val queue = ArrayDeque<String>()
        queue.add(start)
        unvisited.remove(start)
        var size = 0
        while (queue.isNotEmpty()) {
            val state = queue.removeFirst()
            size++
            adjacency[state].orEmpty().forEach { neighbor ->
                if (neighbor in unvisited) {
                    unvisited.remove(neighbor)
                    queue.add(neighbor)
                }
            }
        }
        if (size > largest) largest = size
    }
    return largest
}
