package com.getmecookies.licenseplatequest.ui.map

/**
 * Pure (Color-free, Android-free) color data for the US map, kept separate from [UsMap] so it can be
 * unit-tested on the plain JVM. [StateColorDataTest] verifies that neighboring states never share a
 * color index (four-color theorem; playtest note #6); the adjacency itself lives in
 * [com.getmecookies.licenseplatequest.domain.STATE_ADJACENCY].
 */

/**
 * Per-state index into the 8-color vibrant found palette ([UsMap]'s FOUND_PALETTE), chosen so
 * no two bordering found states share a color — the colorful fill-in mosaic (playtest #6/#14).
 */
internal val STATE_COLOR_INDEX: Map<String, Int> = mapOf(
    "AK" to 1, "AL" to 3, "AR" to 7, "AZ" to 6, "CA" to 0, "CO" to 4, "CT" to 3, "DE" to 6,
    "FL" to 6, "GA" to 1, "HI" to 0, "IA" to 7, "ID" to 4, "IL" to 4, "IN" to 5, "KS" to 2,
    "KY" to 2, "LA" to 2, "MA" to 2, "MD" to 0, "ME" to 7, "MI" to 6, "MN" to 2, "MO" to 1,
    "MS" to 1, "MT" to 0, "NC" to 2, "ND" to 1, "NE" to 3, "NH" to 5, "NJ" to 0, "NM" to 5,
    "NV" to 5, "NY" to 1, "OH" to 4, "OK" to 0, "OR" to 7, "PA" to 7, "RI" to 4, "SC" to 4,
    "SD" to 6, "TN" to 0, "TX" to 6, "UT" to 3, "VA" to 1, "VT" to 7, "WA" to 5, "WI" to 3,
    "WV" to 3, "WY" to 5,
)

/**
 * Per-state index (0..3) into the 4 subtle base tints, so the *unfound* base map is a gentle
 * 4-colored mosaic instead of a flat field — no two neighbors share a base tint (playtest #6).
 */
internal val BASE_COLOR_INDEX: Map<String, Int> = mapOf(
    "AK" to 0, "AL" to 2, "AR" to 2, "AZ" to 2, "CA" to 0, "CO" to 0, "CT" to 2, "DE" to 2,
    "FL" to 1, "GA" to 0, "HI" to 0, "IA" to 1, "ID" to 0, "IL" to 3, "IN" to 0, "KS" to 3,
    "KY" to 2, "LA" to 1, "MA" to 0, "MD" to 1, "ME" to 0, "MI" to 2, "MN" to 2, "MO" to 0,
    "MS" to 0, "MT" to 1, "NC" to 2, "ND" to 3, "NE" to 2, "NH" to 1, "NJ" to 3, "NM" to 3,
    "NV" to 3, "NY" to 1, "OH" to 1, "OK" to 1, "OR" to 1, "PA" to 0, "RI" to 1, "SC" to 1,
    "SD" to 0, "TN" to 1, "TX" to 0, "UT" to 1, "VA" to 0, "VT" to 2, "WA" to 2, "WI" to 0,
    "WV" to 3, "WY" to 3,
)
