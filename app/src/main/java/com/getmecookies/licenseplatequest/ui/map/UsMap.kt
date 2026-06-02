package com.getmecookies.licenseplatequest.ui.map

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.sp
import com.getmecookies.licenseplatequest.R

/**
 * Interactive US map (SPEC section 6 "Active Trip View"). Renders each state from bundled
 * vector paths; found states are filled, unfound states show outline only. Supports
 * pinch-to-zoom and pan, and reports the tapped state via [onStateClick].
 *
 * Coordinate model: state paths live in viewBox space ([UsMapShapes.width] x height). A
 * single affine transform — uniform [scale] plus [offset] — maps viewBox to screen, so a
 * point p draws at p*scale + offset and a tap inverts to (tap - offset) / scale before
 * hit-testing. The transform is initialized to fit-and-center on first layout.
 */
@Composable
fun UsMap(
    shapes: UsMapShapes,
    foundCodes: Set<String>,
    onStateClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    unfoundColor: Color = Color(0xFF33486A),
    outlineColor: Color = Color(0xFF0F1B2D),
    labelColor: Color = Color(0xFFEAF1FB),
) {
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    var scale by remember { mutableFloatStateOf(0f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var minScale by remember { mutableFloatStateOf(0f) }
    val textMeasurer = rememberTextMeasurer()

    // Animate newly-found states from the unfound to the found color (brief fill sweep).
    var previousFound by remember { mutableStateOf(foundCodes) }
    var newlyFound by remember { mutableStateOf(emptySet<String>()) }
    val fillProgress = remember { Animatable(1f) }
    LaunchedEffect(foundCodes) {
        val added = foundCodes - previousFound
        previousFound = foundCodes
        if (added.isNotEmpty()) {
            newlyFound = added
            fillProgress.snapTo(0f)
            fillProgress.animateTo(1f, animationSpec = tween(durationMillis = 450))
        }
    }

    // Fit-and-center once the canvas size and shapes are both known.
    fun ensureInitialized(size: IntSize) {
        if (scale > 0f || size.width == 0 || size.height == 0) return
        val fit = minOf(size.width / shapes.width, size.height / shapes.height)
        minScale = fit
        scale = fit
        offset = Offset(
            x = (size.width - shapes.width * fit) / 2f,
            y = (size.height - shapes.height * fit) / 2f,
        )
    }

    // Keep the map from being panned off-screen: clamp the offset so the scaled content stays
    // within the canvas, centering on any axis where the content is smaller than the canvas.
    fun clampOffset(candidate: Offset, atScale: Float): Offset {
        val cw = canvasSize.width.toFloat()
        val ch = canvasSize.height.toFloat()
        if (cw == 0f || ch == 0f) return candidate
        val contentW = shapes.width * atScale
        val contentH = shapes.height * atScale
        val x = if (contentW <= cw) (cw - contentW) / 2f
        else candidate.x.coerceIn(cw - contentW, 0f)
        val y = if (contentH <= ch) (ch - contentH) / 2f
        else candidate.y.coerceIn(ch - contentH, 0f)
        return Offset(x, y)
    }

    val mapDescription = stringResource(
        R.string.us_map_cd,
        foundCodes.size,
        shapes.states.size,
    )

    Canvas(
        modifier = modifier
            .semantics { contentDescription = mapDescription }
            .pointerInput(shapes) {
                detectTransformGestures { centroid, pan, zoom, _ ->
                    if (scale <= 0f) return@detectTransformGestures
                    val newScale = (scale * zoom).coerceIn(minScale, minScale * 12f)
                    // Keep the gesture centroid anchored, then apply pan, then clamp on-screen.
                    val candidate = (offset - centroid) * (newScale / scale) + centroid + pan
                    offset = clampOffset(candidate, newScale)
                    scale = newScale
                }
            }
            .pointerInput(shapes) {
                detectTapGestures(
                    onTap = onTap@{ tap ->
                        if (scale <= 0f) return@onTap
                        val mapX = (tap.x - offset.x) / scale
                        val mapY = (tap.y - offset.y) / scale
                        shapes.hitTest(mapX, mapY)?.let(onStateClick)
                    },
                    onDoubleTap = onDoubleTap@{ tap ->
                        if (minScale <= 0f) return@onDoubleTap
                        // Toggle: if zoomed in, reset to fit-and-center; else zoom toward the tap.
                        val zoomedIn = scale > minScale * 1.5f
                        if (zoomedIn) {
                            scale = minScale
                            offset = Offset(
                                x = (canvasSize.width - shapes.width * minScale) / 2f,
                                y = (canvasSize.height - shapes.height * minScale) / 2f,
                            )
                        } else {
                            val newScale = (minScale * 3f).coerceAtMost(minScale * 12f)
                            // Keep the tapped screen point fixed as we scale up, then clamp.
                            val candidate = tap - (tap - offset) * (newScale / scale)
                            offset = clampOffset(candidate, newScale)
                            scale = newScale
                        }
                    },
                )
            },
    ) {
        canvasSize = IntSize(size.width.toInt(), size.height.toInt())
        ensureInitialized(canvasSize)
        if (scale <= 0f) return@Canvas

        // Visually-constant ~1.2px outline regardless of zoom (paths draw in scaled space).
        val strokeWidth = 1.2f / scale

        withTransform({
            translate(offset.x, offset.y)
            scale(scale, scale, pivot = Offset.Zero)
        }) {
            shapes.states.forEach { state ->
                // Each found state gets its own vibrant color (stable per state code), so the
                // map fills in as a colorful mosaic; newly-found states animate from unfound.
                val target = foundColorFor(state.code)
                val fill = when {
                    state.code in foundCodes && state.code in newlyFound ->
                        lerp(unfoundColor, target, fillProgress.value)
                    state.code in foundCodes -> target
                    else -> unfoundColor
                }
                drawPath(path = state.path, color = fill)
                drawPath(path = state.path, color = outlineColor, style = Stroke(width = strokeWidth))
            }

            // Color-blind-safe cue: mark found states with a check, not color alone (SPEC §12).
            // Only on the real shapes map; the tile-grid placeholder shows its code label instead.
            if (!shapes.showLabels) {
                shapes.states.forEach { state ->
                    if (state.code !in foundCodes) return@forEach
                    val anchor = state.labelAnchor ?: return@forEach
                    // Pick a dark or light check per state so it stays legible on any fill color.
                    val checkStyle = TextStyle(
                        color = checkColorOn(foundColorFor(state.code)),
                        fontSize = (13f / scale).sp,
                    )
                    val measured = textMeasurer.measure("✓", checkStyle)
                    drawText(
                        textLayoutResult = measured,
                        topLeft = Offset(
                            x = anchor.x - measured.size.width / 2f,
                            y = anchor.y - measured.size.height / 2f,
                        ),
                    )
                }
            }

            if (shapes.showLabels) {
                val labelStyle = TextStyle(
                    color = labelColor,
                    fontSize = (14f / scale).sp,
                )
                shapes.states.forEach { state ->
                    val anchor = state.labelAnchor ?: return@forEach
                    val measured = textMeasurer.measure(state.code, labelStyle)
                    drawText(
                        textLayoutResult = measured,
                        topLeft = Offset(
                            x = anchor.x - measured.size.width / 2f,
                            y = anchor.y - measured.size.height / 2f,
                        ),
                    )
                }
            }
        }
    }
}

/** Vibrant fill colors for found states; each state maps to one deterministically by its code. */
private val FOUND_PALETTE = listOf(
    Color(0xFF06D6A0), // green
    Color(0xFFFFD166), // yellow
    Color(0xFFEF476F), // pink-red
    Color(0xFF4CC9F0), // sky blue
    Color(0xFFF78C6B), // coral
    Color(0xFF9B5DE5), // purple
    Color(0xFF43AA8B), // teal
    Color(0xFFFFB703), // amber
)

/** Stable per-state fill color (same state code always yields the same palette entry). */
private fun foundColorFor(code: String): Color =
    FOUND_PALETTE[(code.hashCode() and 0x7fffffff) % FOUND_PALETTE.size]

/** A dark or light check mark depending on the fill's brightness, so it always reads clearly. */
private fun checkColorOn(fill: Color): Color {
    val luminance = 0.299f * fill.red + 0.587f * fill.green + 0.114f * fill.blue
    return if (luminance > 0.6f) Color(0xFF06231D) else Color(0xFFFFFFFF)
}
