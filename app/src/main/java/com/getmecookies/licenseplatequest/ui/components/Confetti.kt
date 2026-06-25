package com.getmecookies.licenseplatequest.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

/**
 * A self-contained, dependency-free firework burst (pure Compose Canvas). Several bursts ignite
 * in quick succession at scattered points; each throws colorful sparks radially outward that
 * decelerate, sag under gravity, twinkle, and fade. Drive it with a changing [trigger]: each new
 * value restarts the show. Overlay it on a screen (fillMaxSize) above the content; it ignores input.
 *
 * @param trigger restart key — pass a counter that increments per celebration.
 * @param particleCount total number of sparks across all bursts.
 * @param durationMillis how long the whole show lasts.
 * @param particleScale multiplier on spark size — bump it on large surfaces (e.g. an XR panel) so
 *   particles stay big and bold instead of looking tiny and sparse.
 */
@Composable
fun Confetti(
    trigger: Any,
    modifier: Modifier = Modifier,
    particleCount: Int = 160,
    durationMillis: Int = 1800,
    particleScale: Float = 1f,
) {
    val colors = remember {
        listOf(
            Color(0xFF06D6A0), // green
            Color(0xFFFFD166), // yellow
            Color(0xFFEF476F), // pink-red
            Color(0xFF118AB2), // blue
            Color(0xFFF78C6B), // coral
            Color(0xFF9B5DE5), // purple
            Color(0xFF00BBF9), // cyan
            Color(0xFFFFFFFF), // white sparkle
        )
    }

    // Bursts + their sparks are regenerated whenever the trigger changes.
    val show = remember(trigger) {
        val burstCount = (particleCount / 22).coerceIn(4, 7)
        val perBurst = (particleCount / burstCount).coerceAtLeast(10)
        val bursts = List(burstCount) { i ->
            Burst(
                xFraction = 0.18f + Random.nextFloat() * 0.64f,
                yFraction = 0.20f + Random.nextFloat() * 0.45f,
                // Stagger ignition across the first ~55% of the timeline so they go off rapidly.
                startFraction = (i.toFloat() / burstCount) * 0.55f + Random.nextFloat() * 0.04f,
            )
        }
        val sparks = bursts.indices.flatMap { b ->
            List(perBurst) {
                Spark(
                    burst = b,
                    angle = Random.nextFloat() * (2f * Math.PI.toFloat()),
                    speed = 0.11f + Random.nextFloat() * 0.17f,
                    size = 3.5f + Random.nextFloat() * 5f,
                    color = colors[Random.nextInt(colors.size)],
                )
            }
        }
        Show(bursts, sparks)
    }

    var progress by remember(trigger) { mutableFloatStateOf(0f) }

    LaunchedEffect(trigger) {
        progress = 0f
        val start = withFrameNanos { it }
        var now = start
        val durationNanos = durationMillis * 1_000_000L
        while (now - start < durationNanos) {
            now = withFrameNanos { it }
            progress = ((now - start).toFloat() / durationNanos).coerceIn(0f, 1f)
        }
        progress = 1f
    }

    if (progress < 1f) {
        Canvas(modifier = modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val minDim = min(w, h)
            // Each burst lives for this fraction of the timeline after it ignites.
            val burstLife = 0.5f
            show.sparks.forEach { s ->
                val burst = show.bursts[s.burst]
                val local = (progress - burst.startFraction) / burstLife
                if (local < 0f || local > 1f) return@forEach

                // Expand fast then ease out; sag downward as it ages.
                val ease = 1f - (1f - local) * (1f - local)
                val radius = s.speed * ease * minDim
                val gravity = 0.14f * local * local * minDim
                val cx = burst.xFraction * w + cos(s.angle) * radius
                val cy = burst.yFraction * h + sin(s.angle) * radius + gravity

                val alpha = when {
                    local < 0.12f -> local / 0.12f                       // flash in
                    local > 0.55f -> ((1f - local) / 0.45f).coerceIn(0f, 1f) // fade out
                    else -> 1f
                }
                // Twinkle: brief size pulsing as the spark travels.
                val twinkle = 0.75f + 0.25f * sin(local * 22f + s.angle)
                val r = s.size * particleScale * (1f - 0.35f * local) * twinkle
                if (r > 0.3f) {
                    drawCircle(
                        color = s.color.copy(alpha = alpha),
                        radius = r,
                        center = Offset(cx, cy),
                    )
                }
            }
        }
    }
}

private data class Show(
    val bursts: List<Burst>,
    val sparks: List<Spark>,
)

private data class Burst(
    val xFraction: Float,
    val yFraction: Float,
    val startFraction: Float,
)

private data class Spark(
    val burst: Int,
    val angle: Float,
    val speed: Float,
    val size: Float,
    val color: Color,
)
