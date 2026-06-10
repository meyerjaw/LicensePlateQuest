package com.getmecookies.licenseplatequest.domain

import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/** A point in the bundled map's viewBox coordinate space (same space as state centroids/paths). */
data class MapPoint(val x: Float, val y: Float)

/**
 * Projects a geographic coordinate (latitude/longitude, WGS84 degrees) into the bundled US map's
 * viewBox space, so a city can be pinned at its real location on the route (playtest #11 follow-up).
 *
 * The bundled map (`assets/maps/us_states_paths.json`) was produced from the us-atlas
 * `states-albers-10m.json`, i.e. a `d3.geoAlbersUsa()` composite at scale 1300, translate
 * [487.5, 305] (975×610 screen space), then cropped to the states' bounding box with an 8px pad.
 * This reimplements that exact composite (lower-48 Albers + Alaska/Hawaii insets) and applies the
 * same crop shift, so projected points land in the same space as the bundled centroids. Returns
 * null for coordinates outside the composite's clip regions (i.e. not on the US map).
 *
 * Verified offline against the bundled geometry: a spread of known cities each projects inside the
 * correct state polygon (see AlbersUsaProjectionTest for golden values).
 */
object AlbersUsaProjection {

    private const val DEG = Math.PI / 180.0
    private const val K = 1300.0
    private const val TX = 487.5
    private const val TY = 305.0

    // Crop shift the asset build applied: appCoord = albersCoord + (PAD - min), PAD = 8.
    private const val OFFSET_X = 65.6345 // = 8 - (-57.6345)
    private const val OFFSET_Y = -4.9764 // = 8 - 12.9764

    /** One conic-equal-area sub-projection (lower-48, Alaska, or Hawaii). */
    private class Conic(
        private val rotateDeg: Double,
        centerLngDeg: Double,
        centerLatDeg: Double,
        parallel0Deg: Double,
        parallel1Deg: Double,
        private val k: Double,
        private val tx: Double,
        private val ty: Double,
    ) {
        private val n: Double
        private val c: Double
        private val r0: Double
        private val centerX: Double
        private val centerY: Double

        init {
            val sy0 = sin(parallel0Deg * DEG)
            n = (sy0 + sin(parallel1Deg * DEG)) / 2.0
            c = 1 + sy0 * (2 * n - sy0)
            r0 = sqrt(c) / n
            // The center is given in the post-rotation frame, so it is NOT rotated again.
            val (cx, cy) = raw(centerLngDeg * DEG, centerLatDeg * DEG)
            centerX = cx
            centerY = cy
        }

        private fun raw(lambdaRad: Double, phiRad: Double): Pair<Double, Double> {
            val r = sqrt(c - 2 * n * sin(phiRad)) / n
            return r * sin(lambdaRad * n) to r0 - r * cos(lambdaRad * n)
        }

        /** Project to the un-cropped albers screen space (975×610). */
        fun project(lngDeg: Double, latDeg: Double): Pair<Double, Double> {
            val (rx, ry) = raw((lngDeg + rotateDeg) * DEG, latDeg * DEG)
            return tx + k * (rx - centerX) to ty - k * (ry - centerY)
        }
    }

    private val lower48 = Conic(96.0, -0.6, 38.7, 29.5, 45.5, K, TX, TY)
    private val alaska =
        Conic(154.0, -2.0, 58.5, 55.0, 65.0, 0.35 * K, TX - 0.307 * K, TY + 0.201 * K)
    private val hawaii = Conic(157.0, -3.0, 19.9, 8.0, 18.0, K, TX - 0.205 * K, TY + 0.212 * K)

    // Clip rectangles (albers screen space) that decide which inset a point belongs to — matching
    // d3.geoAlbersUsa's routing.
    private val lower48Clip = clip(TX - 0.455 * K, TY - 0.238 * K, TX + 0.455 * K, TY + 0.238 * K)
    private val alaskaClip = clip(TX - 0.425 * K, TY + 0.120 * K, TX - 0.214 * K, TY + 0.234 * K)
    private val hawaiiClip = clip(TX - 0.214 * K, TY + 0.166 * K, TX - 0.115 * K, TY + 0.234 * K)

    private fun clip(x0: Double, y0: Double, x1: Double, y1: Double) = doubleArrayOf(x0, y0, x1, y1)

    private fun Pair<Double, Double>.inside(box: DoubleArray): Boolean =
        first >= box[0] && first <= box[2] && second >= box[1] && second <= box[3]

    /**
     * Project [latDeg]/[lngDeg] into bundled-map viewBox space, or null if it isn't on the US map
     * (outside the lower-48, Alaska, and Hawaii clip regions).
     */
    fun project(latDeg: Double, lngDeg: Double): MapPoint? {
        val screen = lower48.project(lngDeg, latDeg).takeIf { it.inside(lower48Clip) }
            ?: alaska.project(lngDeg, latDeg).takeIf { it.inside(alaskaClip) }
            ?: hawaii.project(lngDeg, latDeg).takeIf { it.inside(hawaiiClip) }
            ?: return null
        return MapPoint((screen.first + OFFSET_X).toFloat(), (screen.second + OFFSET_Y).toFloat())
    }
}
