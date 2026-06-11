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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

/**
 * A bold, gold **rare-plate** flourish — deliberately distinct from the colorful [Confetti] firework
 * and big enough not to miss. A bright golden glow blooms from the center, a gold ring bursts
 * outward, a large 8-point "hero" star pops in (with overshoot) and lingers, and a scatter of
 * twinkling stars drift up and out. Pure Compose Canvas, no dependencies, ignores input. Drive it
 * with a changing [trigger]; each new value restarts the sparkle.
 *
 * @param trigger restart key — pass a counter that increments per rare catch.
 * @param durationMillis how long the flourish lasts.
 */
@Composable
fun RareSparkle(
    trigger: Any,
    modifier: Modifier = Modifier,
    sparkleCount: Int = 40,
    durationMillis: Int = 1600,
) {
    val colors = remember {
        listOf(
            Color(0xFFFFD166), // gold
            Color(0xFFFFB703), // amber
            Color(0xFFFFE9A8), // light gold
            Color(0xFFFFFFFF), // white sparkle
        )
    }

    val sparkles = remember(trigger) {
        List(sparkleCount) {
            Sparkle(
                angle = Random.nextFloat() * (2f * Math.PI.toFloat()),
                speed = 0.16f + Random.nextFloat() * 0.30f,
                size = 6f + Random.nextFloat() * 10f,
                start = Random.nextFloat() * 0.5f,
                phase = Random.nextFloat() * (2f * Math.PI.toFloat()),
                color = colors[Random.nextInt(colors.size)],
            )
        }
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
            val cx0 = w * 0.5f
            val cy0 = h * 0.42f
            val gold = Color(0xFFFFD166)

            // 1) Bright central glow that blooms then fades over the first ~60%.
            val glow = (1f - (progress / 0.6f)).coerceIn(0f, 1f)
            if (glow > 0f) {
                val rGlow = (0.18f + 0.55f * (progress / 0.4f).coerceAtMost(1f)) * minDim
                drawCircle(gold.copy(alpha = 0.42f * glow), rGlow, Offset(cx0, cy0))
                drawCircle(Color.White.copy(alpha = 0.30f * glow), rGlow * 0.45f, Offset(cx0, cy0))
            }

            // 2) Gold ring bursting outward in the first ~55%.
            val ringT = (progress / 0.55f).coerceIn(0f, 1f)
            if (ringT < 1f) {
                val ease = 1f - (1f - ringT) * (1f - ringT)
                drawCircle(
                    color = gold.copy(alpha = (1f - ringT) * 0.9f),
                    radius = ease * 0.7f * minDim,
                    center = Offset(cx0, cy0),
                    style = Stroke(
                        width = (6f * (1f - ringT) + 1.5f) * (minDim / 360f).coerceAtLeast(
                            1f
                        )
                    ),
                )
            }

            // 3) Hero 8-point star: pops in with overshoot, holds, fades after ~65%.
            val popT = ((progress - 0.04f) / 0.20f).coerceIn(0f, 1f)
            val pop = easeOutBack(popT)
            val heroAlpha = when {
                progress < 0.04f -> 0f
                progress > 0.7f -> ((1f - progress) / 0.3f).coerceIn(0f, 1f)
                else -> 1f
            }
            if (heroAlpha > 0f) {
                val arm = pop * 0.13f * minDim
                drawStar8(Offset(cx0, cy0), arm, gold.copy(alpha = heroAlpha))
                drawCircle(Color.White.copy(alpha = heroAlpha), arm * 0.22f, Offset(cx0, cy0))
            }

            // 4) Twinkling sparkles drifting up and out.
            val life = 0.6f
            sparkles.forEach { s ->
                val local = (progress - s.start) / life
                if (local < 0f || local > 1f) return@forEach
                val ease = 1f - (1f - local) * (1f - local)
                val radius = s.speed * ease * minDim
                val rise = 0.12f * local * minDim
                val cx = cx0 + cos(s.angle) * radius
                val cy = cy0 + sin(s.angle) * radius - rise
                val alpha = when {
                    local < 0.12f -> local / 0.12f
                    local > 0.6f -> ((1f - local) / 0.4f).coerceIn(0f, 1f)
                    else -> 1f
                }
                val twinkle = 0.55f + 0.45f * sin(local * 18f + s.phase)
                val armS = s.size * (1f - 0.3f * local) * (0.7f + 0.6f * twinkle)
                if (armS > 0.5f) {
                    drawSparkle(
                        Offset(cx, cy),
                        armS,
                        s.color.copy(alpha = alpha * (0.6f + 0.4f * twinkle))
                    )
                }
            }
        }
    }
}

/** easeOutBack — overshoots past 1 then settles, for a satisfying "pop". */
private fun easeOutBack(t: Float): Float {
    val s = 1.70158f
    val u = t - 1f
    return u * u * ((s + 1f) * u + s) + 1f
}

/** A 4-point sparkle: two crossed needles plus a bright core. */
private fun DrawScope.drawSparkle(center: Offset, arm: Float, color: Color) {
    val stroke = (arm * 0.22f).coerceAtLeast(1f)
    drawLine(
        color,
        Offset(center.x, center.y - arm),
        Offset(center.x, center.y + arm),
        stroke,
        StrokeCap.Round
    )
    drawLine(
        color,
        Offset(center.x - arm, center.y),
        Offset(center.x + arm, center.y),
        stroke,
        StrokeCap.Round
    )
    drawCircle(color = color, radius = arm * 0.26f, center = center)
}

/** An 8-point star: a long + cross plus a shorter × cross. */
private fun DrawScope.drawStar8(center: Offset, arm: Float, color: Color) {
    val stroke = (arm * 0.16f).coerceAtLeast(1.5f)
    // + arms
    drawLine(
        color,
        Offset(center.x, center.y - arm),
        Offset(center.x, center.y + arm),
        stroke,
        StrokeCap.Round
    )
    drawLine(
        color,
        Offset(center.x - arm, center.y),
        Offset(center.x + arm, center.y),
        stroke,
        StrokeCap.Round
    )
    // × arms (shorter)
    val d = arm * 0.62f * 0.7071f
    drawLine(
        color,
        Offset(center.x - d, center.y - d),
        Offset(center.x + d, center.y + d),
        stroke * 0.8f,
        StrokeCap.Round
    )
    drawLine(
        color,
        Offset(center.x - d, center.y + d),
        Offset(center.x + d, center.y - d),
        stroke * 0.8f,
        StrokeCap.Round
    )
}

private data class Sparkle(
    val angle: Float,
    val speed: Float,
    val size: Float,
    val start: Float,
    val phase: Float,
    val color: Color,
)
