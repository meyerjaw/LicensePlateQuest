package com.getmecookies.licenseplatequest.ui.map

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.ui.Modifier
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.sp

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
    foundColor: Color = Color(0xFF2A9D8F),
    unfoundColor: Color = Color(0xFF33486A),
    outlineColor: Color = Color(0xFF0F1B2D),
    labelColor: Color = Color(0xFFEAF1FB),
) {
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    var scale by remember { mutableFloatStateOf(0f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var minScale by remember { mutableFloatStateOf(0f) }
    val textMeasurer = rememberTextMeasurer()

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

    Canvas(
        modifier = modifier
            .pointerInput(shapes) {
                detectTransformGestures { centroid, pan, zoom, _ ->
                    if (scale <= 0f) return@detectTransformGestures
                    val newScale = (scale * zoom).coerceIn(minScale, minScale * 12f)
                    // Keep the gesture centroid anchored, then apply pan.
                    offset = (offset - centroid) * (newScale / scale) + centroid + pan
                    scale = newScale
                }
            }
            .pointerInput(shapes) {
                detectTapGestures { tap ->
                    if (scale <= 0f) return@detectTapGestures
                    val mapX = (tap.x - offset.x) / scale
                    val mapY = (tap.y - offset.y) / scale
                    shapes.hitTest(mapX, mapY)?.let(onStateClick)
                }
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
                val fill = if (state.code in foundCodes) foundColor else unfoundColor
                drawPath(path = state.path, color = fill)
                drawPath(path = state.path, color = outlineColor, style = Stroke(width = strokeWidth))
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
