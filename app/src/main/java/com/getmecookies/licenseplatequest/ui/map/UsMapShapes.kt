package com.getmecookies.licenseplatequest.ui.map

import android.graphics.Region
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path

/**
 * A single state's geometry, ready for both drawing and hit-testing in map (viewBox) space.
 *
 * @property code 2-letter state code (e.g. "OH").
 * @property path Compose path for drawing the outline/fill.
 * @property region integer-rasterized region of the same shape, used for point-in-polygon
 *   tap detection (handles multi-polygon states like Hawaii correctly).
 * @property labelAnchor where to draw the state-code label, in viewBox space, or null.
 */
class StateShape(
    val code: String,
    val path: Path,
    val region: Region,
    val labelAnchor: Offset?,
)

/**
 * All 50 state shapes plus the coordinate space they're defined in. The map composable scales
 * this [width] x [height] viewBox to fit the screen; taps are converted back into this space
 * before hit-testing against each [StateShape.region].
 */
class UsMapShapes(
    val width: Float,
    val height: Float,
    val states: List<StateShape>,
    /** True for the offline tile-grid placeholder; drives label rendering. */
    val showLabels: Boolean = false,
) {
    /** The state whose region contains the given point in viewBox space, or null. */
    fun hitTest(x: Float, y: Float): String? {
        val xi = x.toInt()
        val yi = y.toInt()
        // Iterate in reverse so smaller states drawn later win ties on shared borders.
        for (i in states.indices.reversed()) {
            if (states[i].region.contains(xi, yi)) return states[i].code
        }
        return null
    }
}
