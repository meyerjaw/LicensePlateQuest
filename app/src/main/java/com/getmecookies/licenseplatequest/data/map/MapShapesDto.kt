package com.getmecookies.licenseplatequest.data.map

import kotlinx.serialization.Serializable

/**
 * Wire model for the bundled `assets/maps/us_states_paths.json` (Milestone 5). The file maps
 * each 2-letter state code to an SVG path string (commands M/L/Z only) in a fixed viewBox.
 *
 * [style] identifies the map representation: "tile-grid" is the offline placeholder (one
 * square per state in its approximate geographic position) shipped while accurate boundary
 * data is unavailable; swapping in a realistic geoAlbersUsa-projected file needs no code
 * changes. [centroids] gives a label anchor per state ([x, y] in viewBox space).
 */
@Serializable
data class MapShapesDto(
    val viewBox: ViewBoxDto,
    val states: Map<String, String>,
    val centroids: Map<String, List<Float>> = emptyMap(),
    val style: String = "shapes",
    val count: Int = 0,
)

@Serializable
data class ViewBoxDto(
    val width: Float,
    val height: Float,
)
