package com.getmecookies.licenseplatequest.ui.map

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.withResumed
import com.getmecookies.licenseplatequest.R
import com.getmecookies.licenseplatequest.domain.MapPoint

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
    interactive: Boolean = true,
    // Themed to the app palette so unfound states/outlines/labels match light or dark mode,
    // rather than a fixed slate (the vibrant found-state palette stays as-is on top).
    unfoundColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    outlineColor: Color = MaterialTheme.colorScheme.outline,
    labelColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    routeStops: List<String> = emptyList(),
    /**
     * Per-stop real-city positions (parallel to [routeStops], map viewBox coords); null = pin the
     * state center instead. Lets the route follow the actual cities (playtest #11 follow-up).
     */
    routeCityPoints: List<MapPoint?> = emptyList(),
    routeColor: Color = MaterialTheme.colorScheme.primary,
    // Found states awaiting their fill animation (playtest #20). The map animates these, then calls
    // [onCelebrated] so they're stamped done and won't replay — even finds made off the map.
    celebrateCodes: Set<String> = emptySet(),
    onCelebrated: (Set<String>) -> Unit = {},
) {
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    var scale by remember { mutableFloatStateOf(0f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var minScale by remember { mutableFloatStateOf(0f) }
    val textMeasurer = rememberTextMeasurer()

    // Animate pending finds from the base to the found color (fill sweep), then acknowledge so
    // they're stamped celebrated. Driven by [celebrateCodes] (persisted), so finds made while the map
    // wasn't on screen still play their sweep on the next visit (playtest #20).
    val lifecycleOwner = LocalLifecycleOwner.current
    var newlyFound by remember { mutableStateOf(emptySet<String>()) }
    val fillProgress = remember { Animatable(1f) }
    LaunchedEffect(celebrateCodes) {
        if (celebrateCodes.isNotEmpty()) {
            val animating = celebrateCodes
            // Reset these to the base color *first*: foundCodes already contains them, so they'd
            // otherwise render fully filled and then visibly "unfill" the moment we snap to 0. Doing
            // it before the wait means that reset happens behind the nav transition, unseen.
            newlyFound = animating
            fillProgress.snapTo(0f)
            // Then wait until the map is actually on screen before sweeping. Returning from State
            // Detail runs a nav transition during which this composable is only STARTED, not RESUMED
            // — animating then would finish the sweep behind the transition and never be seen (a tab
            // switch works without this only because it has no transition). Resolves immediately when
            // already resumed (on-map find / tab switch), so those are unchanged.
            lifecycleOwner.lifecycle.withResumed {}
            fillProgress.animateTo(1f, animationSpec = tween(durationMillis = STATE_FILL_ANIM_MS))
            newlyFound = emptySet()
            onCelebrated(animating)
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
            // Pan/zoom + tap only when interactive; the display-only summary map must let the
            // parent scroll pass through (playtest note #3).
            .then(
                if (!interactive) Modifier else Modifier
                    .pointerInput(shapes) {
                        detectTransformGestures { centroid, pan, zoom, _ ->
                            if (scale <= 0f) return@detectTransformGestures
                            val newScale = (scale * zoom).coerceIn(minScale, minScale * 12f)
                            // Keep the gesture centroid anchored, then apply pan, then clamp on-screen.
                            val candidate =
                                (offset - centroid) * (newScale / scale) + centroid + pan
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
                                // ~16dp of slop (converted to viewBox units) so tiny states are tappable.
                                shapes.hitTest(mapX, mapY, tolerance = 16f / scale)
                                    ?.let(onStateClick)
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
            ),
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
                val base = baseColorFor(state.code, unfoundColor)
                val fill = when {
                    state.code in foundCodes && state.code in newlyFound ->
                        lerp(base, target, fillProgress.value)
                    state.code in foundCodes -> target
                    else -> base
                }
                drawPath(path = state.path, color = fill)
                drawPath(path = state.path, color = outlineColor, style = Stroke(width = strokeWidth))
            }

            // Color-blind-safe cue: mark found states with a check, not color alone (SPEC §12).
            // Only on the real shapes map; the tile-grid placeholder shows its code label instead.
            if (!shapes.showLabels) {
                shapes.states.forEach { state ->
                    val anchor = state.labelAnchor ?: return@forEach
                    if (state.code in foundCodes) {
                        // Found: a dark/light check per state so it reads on any fill color.
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
                    } else {
                        // Unfound: the USPS abbreviation at the state's visual center, sized to the
                        // state's bounding box so big states read large and tiny ones stay
                        // contained (playtest note #10). Shares the anchor with the check, so on
                        // mark-as-found the code visually swaps to a check in the same spot.
                        val bounds = state.path.getBounds()
                        val sizeVb = (minOf(bounds.width, bounds.height) * 0.16f).coerceIn(2f, 9f)
                        val abbrStyle = TextStyle(
                            color = labelColor,
                            fontSize = sizeVb.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        val measured = textMeasurer.measure(state.code, abbrStyle)
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

            // Route overlay (playtest #11): the trip's stops in order, joined by a connecting
            // line with numbered pins at each state's visual center. Drawn last, so the pins sit
            // on top of any check marks/labels. Sizes use 1/scale to stay visually constant.
            if (routeStops.isNotEmpty()) {
                val anchorByCode = shapes.states.associate { it.code to it.labelAnchor }
                // Pin each stop at its geocoded city when available, else the state's center.
                val routeAnchors = routeStops.mapIndexedNotNull { i, code ->
                    routeCityPoints.getOrNull(i)?.let { Offset(it.x, it.y) } ?: anchorByCode[code]
                }
                for (i in 0 until routeAnchors.size - 1) {
                    drawLine(
                        color = routeColor,
                        start = routeAnchors[i],
                        end = routeAnchors[i + 1],
                        strokeWidth = 3f / scale,
                        cap = StrokeCap.Round,
                    )
                }
                val pinStyle = TextStyle(
                    color = Color.White,
                    fontSize = (9f / scale).sp,
                    fontWeight = FontWeight.Bold,
                )
                routeAnchors.forEachIndexed { index, anchor ->
                    drawCircle(color = routeColor, radius = 9f / scale, center = anchor)
                    val measured = textMeasurer.measure((index + 1).toString(), pinStyle)
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

/** Duration of the per-state fill sweep when a find is celebrated on the map (playtest #20). */
private const val STATE_FILL_ANIM_MS = 850

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

/**
 * Stable per-state fill color. Neighboring states are guaranteed different via [STATE_COLOR_INDEX]
 * (graph-colored in [StateColorData]); any code outside the bundled 50 (e.g. future DC/territories)
 * falls back to a hash so it still gets a stable color.
 */
private fun foundColorFor(code: String): Color {
    val index = STATE_COLOR_INDEX[code] ?: ((code.hashCode() and 0x7fffffff) % FOUND_PALETTE.size)
    return FOUND_PALETTE[index]
}

/**
 * The vibrant accent color for a state — the same per-state hue used on the found map mosaic — so
 * lists and cards elsewhere can echo the map. Stable per state code.
 */
fun stateAccentColor(code: String): Color = foundColorFor(code)

/**
 * Four subtle hues for the *unfound* base map (playtest note #6). Kept gentle so the base reads as a
 * soft mosaic, not a loud one — the vibrant found palette still pops on top when a state is marked.
 */
private val BASE_TINT_PALETTE = listOf(
    Color(0xFF80CBC4), // soft teal
    Color(0xFFFFE0A3), // soft amber
    Color(0xFFF5B7C4), // soft rose
    Color(0xFFB3C7E6), // soft periwinkle
)

/**
 * The unfound base fill: the themed [neutral] gently tinted toward one of four hues so neighboring
 * unfound states differ subtly ([BASE_COLOR_INDEX] guarantees no two neighbors share a tint).
 * Falls back to the plain neutral for any code outside the bundled 50.
 */
private fun baseColorFor(code: String, neutral: Color): Color {
    val index = BASE_COLOR_INDEX[code] ?: return neutral
    return lerp(neutral, BASE_TINT_PALETTE[index], 0.15f)
}

/** A dark or light check mark depending on the fill's brightness, so it always reads clearly. */
private fun checkColorOn(fill: Color): Color {
    val luminance = 0.299f * fill.red + 0.587f * fill.green + 0.114f * fill.blue
    return if (luminance > 0.6f) Color(0xFF06231D) else Color(0xFFFFFFFF)
}
