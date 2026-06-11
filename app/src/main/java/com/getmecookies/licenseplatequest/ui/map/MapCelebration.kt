package com.getmecookies.licenseplatequest.ui.map

/**
 * Timing for the deferred find-celebration fill sweep (playtest #20). A single find sweeps on its
 * own; a small batch (2–5) plays as a **staggered cascade** so the fills ripple instead of flashing
 * together; a large batch (6+) plays as a fast **combo** and asks the UI to show a "+N states!"
 * overlay. Pure + unit-tested so the thresholds/feel are easy to tune without touching the renderer.
 */
data class CelebrationTiming(
    /** Delay between successive states' fills starting (ms). 0 = all start together. */
    val staggerMs: Int,
    /** Duration of a single state's fill sweep (ms). */
    val fillMs: Int,
    /** Whether to show the "+N states!" combo overlay. */
    val combo: Boolean,
    /** Extra time the clock runs after the last fill, so the combo overlay lingers (ms). */
    val holdMs: Int = 0,
) {
    /**
     * Total animation time for [count] finds: the last fill starts at (count-1)·stagger, runs for
     * fillMs, then the overlay holds for holdMs.
     */
    fun totalMs(count: Int): Int = (count - 1).coerceAtLeast(0) * staggerMs + fillMs + holdMs
}

/** Pick the celebration timing for a batch of [count] queued finds. */
fun celebrationTiming(count: Int): CelebrationTiming = when {
    count >= 6 -> CelebrationTiming(staggerMs = 55, fillMs = 560, combo = true, holdMs = 700)
    count >= 2 -> CelebrationTiming(staggerMs = 130, fillMs = 880, combo = false)
    else -> CelebrationTiming(staggerMs = 0, fillMs = 950, combo = false)
}
