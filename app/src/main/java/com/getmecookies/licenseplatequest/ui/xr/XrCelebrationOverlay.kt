package com.getmecookies.licenseplatequest.ui.xr

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.getmecookies.licenseplatequest.ui.components.Confetti

/**
 * Content for the big, transparent "spatial confetti" panel on Android XR (experimental). Reuses the
 * in-app [Confetti] Canvas, drawn on a transparent background so only the particles show — letting a
 * celebration burst across the spatial view instead of being clipped to the app panel.
 *
 * Driven by [trigger]: bump it (a counter) to fire a burst; 0 means idle (nothing drawn).
 */
@Composable
fun XrCelebrationOverlay(
    trigger: Int,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        if (trigger > 0) {
            // Big, dense, long bursts so the celebration reads boldly across the spatial view
            // (a normal-scale Confetti looks tiny/sparse spread over a large panel).
            Confetti(
                trigger = trigger,
                particleCount = 320,
                durationMillis = 2800,
                particleScale = 5f,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
