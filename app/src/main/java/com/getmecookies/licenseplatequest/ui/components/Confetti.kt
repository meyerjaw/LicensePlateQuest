package com.getmecookies.licenseplatequest.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import kotlin.random.Random

/**
 * A self-contained, dependency-free confetti burst (pure Compose Canvas). Particles spawn at
 * the top, fall with gravity and horizontal drift, spin, and fade near the end. Drive it with
 * a changing [trigger]: each new value restarts the animation. Overlay it on a screen (e.g.
 * fillMaxSize) above the content; it ignores input.
 *
 * @param trigger restart key — pass a counter that increments per celebration.
 * @param particleCount number of confetti pieces.
 * @param durationMillis how long the burst lasts.
 */
@Composable
fun Confetti(
    trigger: Any,
    modifier: Modifier = Modifier,
    particleCount: Int = 120,
    durationMillis: Int = 2200,
) {
    val colors = remember {
        listOf(
            Color(0xFFFFD166),
            Color(0xFF2A9D8F),
            Color(0xFFE76F51),
            Color(0xFF457B9D),
            Color(0xFFE9C46A),
            Color(0xFFF4A261),
        )
    }

    // Particles are regenerated whenever the trigger changes.
    val particles = remember(trigger) {
        List(particleCount) {
            Particle(
                startXFraction = Random.nextFloat(),
                startYFraction = Random.nextFloat() * 0.2f - 0.2f, // start just above the top
                horizontalDrift = (Random.nextFloat() - 0.5f) * 0.4f,
                fallSpeed = 0.6f + Random.nextFloat() * 0.8f,
                size = 6f + Random.nextFloat() * 8f,
                color = colors[Random.nextInt(colors.size)],
                rotationSpeed = (Random.nextFloat() - 0.5f) * 720f,
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
            particles.forEach { p ->
                val x = (p.startXFraction + p.horizontalDrift * progress) * w
                val y = (p.startYFraction + p.fallSpeed * progress) * (h * 1.3f)
                if (y in -p.size..h + p.size) {
                    val alpha = if (progress > 0.8f) ((1f - progress) / 0.2f).coerceIn(0f, 1f) else 1f
                    drawRect(
                        color = p.color.copy(alpha = alpha),
                        topLeft = Offset(x, y),
                        size = Size(p.size, p.size * 0.5f),
                    )
                }
            }
        }
    }
}

private data class Particle(
    val startXFraction: Float,
    val startYFraction: Float,
    val horizontalDrift: Float,
    val fallSpeed: Float,
    val size: Float,
    val color: Color,
    val rotationSpeed: Float,
)
