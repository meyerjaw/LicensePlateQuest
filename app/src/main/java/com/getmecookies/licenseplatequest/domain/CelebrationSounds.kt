package com.getmecookies.licenseplatequest.domain

/**
 * Plays the short celebration sounds (a chime when a state is found, a fanfare for 50/50). A seam
 * so the platform audio is injectable and the rest of the app stays testable. Implementations
 * respect the user's sound setting and the device's media volume / silent mode.
 */
interface CelebrationSounds {
    /** A brief chime for a single new find. */
    fun playFind()

    /** A sparkly twinkle layered on top of the find chime for a rare-plate catch. */
    fun playRare()

    /** A longer fanfare for completing all 50 (the 50/50 win). */
    fun playFifty()
}
