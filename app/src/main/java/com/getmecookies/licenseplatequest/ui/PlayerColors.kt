package com.getmecookies.licenseplatequest.ui

import androidx.compose.ui.graphics.Color

/** One swatch in the curated player-color palette (playtest note #19). */
data class PlayerSwatch(val token: String, val color: Color)

/**
 * Curated player-color palette and resolution helpers (playtest note #19). Players store a color
 * token; the UI resolves it to a [Color]. A player with no chosen color (null token) still gets a
 * stable, distinct color derived from a key (their id), so nobody is colorless.
 */
object PlayerColors {
    val palette: List<PlayerSwatch> = listOf(
        PlayerSwatch("red", Color(0xFFEF476F)),
        PlayerSwatch("orange", Color(0xFFF78C6B)),
        PlayerSwatch("amber", Color(0xFFFFB703)),
        PlayerSwatch("yellow", Color(0xFFFFD166)),
        PlayerSwatch("green", Color(0xFF06D6A0)),
        PlayerSwatch("teal", Color(0xFF43AA8B)),
        PlayerSwatch("blue", Color(0xFF4CC9F0)),
        PlayerSwatch("indigo", Color(0xFF4361EE)),
        PlayerSwatch("purple", Color(0xFF9B5DE5)),
        PlayerSwatch("pink", Color(0xFFE85D9C)),
    )

    /** Resolve a stored token to a color, falling back to a stable per-[fallbackKey] color. */
    fun resolve(token: String?, fallbackKey: String): Color {
        palette.firstOrNull { it.token == token }?.let { return it.color }
        val index = (fallbackKey.hashCode() and 0x7fffffff) % palette.size
        return palette[index].color
    }

    /** The first palette token not already in [usedTokens], or the first token if all are used. */
    fun firstUnusedToken(usedTokens: Collection<String?>): String {
        val used = usedTokens.filterNotNull().toSet()
        return palette.firstOrNull { it.token !in used }?.token ?: palette.first().token
    }
}
