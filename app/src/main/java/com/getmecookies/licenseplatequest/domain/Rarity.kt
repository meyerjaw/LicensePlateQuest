package com.getmecookies.licenseplatequest.domain

/**
 * A plate is "rare" when its bundled `rarity_score` clears this threshold. Tuned from the bundled
 * data (scores 0.1–0.95): 0.6 flags ~6 states (HI, AK, ND, WY, VT, SD) — special without being
 * common. Spotting one earns a little extra fanfare (playtest: rare-plate moments).
 */
const val RARE_PLATE_THRESHOLD = 0.6

/** Whether a state counts as a rare find, by its bundled rarity score. */
fun isRarePlate(rarityScore: Double): Boolean = rarityScore >= RARE_PLATE_THRESHOLD
