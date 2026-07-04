package com.getmecookies.licenseplatequest.domain.plate

import kotlin.math.max

/**
 * The best state guess for a set of OCR text lines read off a license plate.
 *
 * @param stateCode 2-letter USPS code of the matched state.
 * @param confidence 0..1 — how good the match was (1 = an exact phrase hit).
 * @param matchedPhrase the dictionary phrase that matched (for debugging/telemetry).
 */
data class PlateMatch(
    val stateCode: String,
    val confidence: Float,
    val matchedPhrase: String,
)

/**
 * Pure, Android-free logic that maps text recognized on a plate (from ML Kit, Gemini, or a test) to
 * the issuing **state** — the whole point of the recognizer, and the part worth unit-testing offline.
 *
 * It matches against a bundled dictionary of, per state, its **name** and well-known **plate slogans**
 * (e.g. "SUNSHINE STATE" → FL). Matching normalizes case/punctuation, allows a phrase to appear
 * embedded in a longer line, and tolerates OCR noise via a light fuzzy (edit-distance) pass. It never
 * reads or returns the plate number.
 *
 * Deliberately does NOT match bare 2-letter codes — too many false positives against random plate
 * characters. Thresholds are heuristics to tune against real-world footage (the Phase 0 spike).
 */
object PlateStateMatcher {

    /** Minimum confidence to accept a match (tune with real data). */
    const val ACCEPT_THRESHOLD = 0.78f

    private const val NAME_WEIGHT = 1.0f
    private const val SLOGAN_WEIGHT = 0.92f

    /** Best state match across all [recognizedLines], or null if nothing clears the threshold. */
    fun match(recognizedLines: List<String>): PlateMatch? {
        val text = recognizedLines.joinToString(" ") { normalize(it) }.trim()
        if (text.isBlank()) return null

        var best: PlateMatch? = null
        for (phrase in DICTIONARY) {
            val score = containmentScore(text, phrase.text) * phrase.weight
            if (score >= ACCEPT_THRESHOLD && (best == null || score > best.confidence)) {
                best = PlateMatch(phrase.stateCode, score.coerceAtMost(1f), phrase.text)
            }
        }
        return best
    }

    // --- Text scoring ---------------------------------------------------------------------------

    /** Uppercase, keep A–Z and digits and single spaces. */
    private fun normalize(raw: String): String =
        raw.uppercase()
            .map { if (it.isLetterOrDigit()) it else ' ' }
            .joinToString("")
            .replace(Regex("\\s+"), " ")
            .trim()

    /**
     * How well [phrase] is contained in [text]: 1.0 for an exact substring, otherwise the best
     * fuzzy (1 − editDistance/len) over sliding windows of the phrase's length — so a couple of OCR
     * character errors still score high on longer phrases.
     */
    private fun containmentScore(text: String, phrase: String): Float {
        if (phrase.isEmpty()) return 0f
        if (text.contains(phrase)) return 1f
        val p = phrase.length
        if (text.length < p) return similarity(text, phrase)
        var best = 0f
        for (i in 0..text.length - p) {
            val s = similarity(text.substring(i, i + p), phrase)
            if (s > best) best = s
            if (best == 1f) break
        }
        return best
    }

    private fun similarity(a: String, b: String): Float {
        val maxLen = max(a.length, b.length)
        if (maxLen == 0) return 1f
        return 1f - levenshtein(a, b).toFloat() / maxLen
    }

    private fun levenshtein(a: String, b: String): Int {
        val prev = IntArray(b.length + 1) { it }
        val curr = IntArray(b.length + 1)
        for (i in 1..a.length) {
            curr[0] = i
            for (j in 1..b.length) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                curr[j] = minOf(prev[j] + 1, curr[j - 1] + 1, prev[j - 1] + cost)
            }
            prev.indices.forEach { prev[it] = curr[it] }
        }
        return prev[b.length]
    }

    // --- Dictionary -----------------------------------------------------------------------------

    private class Phrase(val text: String, val stateCode: String, val weight: Float)

    private class StateEntry(
        val code: String,
        val name: String,
        val slogans: List<String> = emptyList()
    )

    // lazy so it's built on first use — after STATES (declared below) is initialized. Object property
    // initializers otherwise run top-to-bottom, and DICTIONARY is declared before STATES.
    // Sorted longest-phrase-first so a more specific phrase wins ties over one it contains — e.g.
    // "WEST VIRGINIA" (WV) beats the substring "VIRGINIA" (VA), which both match exactly.
    private val DICTIONARY: List<Phrase> by lazy {
        buildList {
            for (s in STATES) {
                add(Phrase(s.name, s.code, NAME_WEIGHT))
                for (slogan in s.slogans) add(Phrase(slogan, s.code, SLOGAN_WEIGHT))
            }
        }.sortedByDescending { it.text.length }
    }

    // Names + well-known plate slogans. Slogans are as they appear on plates (normalized to letters).
    private val STATES = listOf(
        StateEntry("AL", "ALABAMA", listOf("SWEET HOME ALABAMA", "HEART OF DIXIE")),
        StateEntry("AK", "ALASKA", listOf("THE LAST FRONTIER")),
        StateEntry("AZ", "ARIZONA", listOf("GRAND CANYON STATE")),
        StateEntry("AR", "ARKANSAS", listOf("THE NATURAL STATE", "LAND OF OPPORTUNITY")),
        StateEntry("CA", "CALIFORNIA"),
        StateEntry("CO", "COLORADO", listOf("COLORFUL COLORADO")),
        StateEntry("CT", "CONNECTICUT", listOf("CONSTITUTION STATE")),
        StateEntry("DE", "DELAWARE", listOf("THE FIRST STATE")),
        StateEntry("FL", "FLORIDA", listOf("SUNSHINE STATE")),
        StateEntry("GA", "GEORGIA", listOf("PEACH STATE")),
        StateEntry("HI", "HAWAII", listOf("ALOHA STATE")),
        StateEntry("ID", "IDAHO", listOf("FAMOUS POTATOES", "SCENIC IDAHO")),
        StateEntry("IL", "ILLINOIS", listOf("LAND OF LINCOLN")),
        StateEntry("IN", "INDIANA", listOf("HOOSIER STATE", "IN GOD WE TRUST")),
        StateEntry("IA", "IOWA"),
        StateEntry("KS", "KANSAS"),
        StateEntry("KY", "KENTUCKY", listOf("UNBRIDLED SPIRIT", "BLUEGRASS STATE")),
        StateEntry("LA", "LOUISIANA", listOf("SPORTSMANS PARADISE")),
        StateEntry("ME", "MAINE", listOf("VACATIONLAND")),
        StateEntry("MD", "MARYLAND"),
        StateEntry("MA", "MASSACHUSETTS", listOf("THE SPIRIT OF AMERICA")),
        StateEntry("MI", "MICHIGAN", listOf("PURE MICHIGAN", "GREAT LAKES")),
        StateEntry("MN", "MINNESOTA", listOf("EXPLORE MINNESOTA", "10000 LAKES")),
        StateEntry("MS", "MISSISSIPPI"),
        StateEntry("MO", "MISSOURI", listOf("SHOW ME STATE")),
        StateEntry("MT", "MONTANA", listOf("BIG SKY COUNTRY", "BIG SKY")),
        StateEntry("NE", "NEBRASKA", listOf("THE GOOD LIFE")),
        StateEntry("NV", "NEVADA", listOf("THE SILVER STATE")),
        StateEntry("NH", "NEW HAMPSHIRE", listOf("LIVE FREE OR DIE")),
        StateEntry("NJ", "NEW JERSEY", listOf("GARDEN STATE")),
        StateEntry("NM", "NEW MEXICO", listOf("LAND OF ENCHANTMENT")),
        StateEntry("NY", "NEW YORK", listOf("EMPIRE STATE", "EXCELSIOR")),
        StateEntry("NC", "NORTH CAROLINA", listOf("FIRST IN FLIGHT")),
        StateEntry("ND", "NORTH DAKOTA", listOf("PEACE GARDEN STATE", "LEGENDARY")),
        StateEntry("OH", "OHIO", listOf("BIRTHPLACE OF AVIATION")),
        StateEntry("OK", "OKLAHOMA", listOf("NATIVE AMERICA")),
        StateEntry("OR", "OREGON"),
        StateEntry("PA", "PENNSYLVANIA"),
        StateEntry("RI", "RHODE ISLAND", listOf("OCEAN STATE")),
        StateEntry("SC", "SOUTH CAROLINA"),
        StateEntry(
            "SD",
            "SOUTH DAKOTA",
            listOf("GREAT FACES GREAT PLACES", "MOUNT RUSHMORE STATE")
        ),
        StateEntry("TN", "TENNESSEE", listOf("THE VOLUNTEER STATE")),
        StateEntry("TX", "TEXAS", listOf("THE LONE STAR STATE")),
        StateEntry("UT", "UTAH", listOf("LIFE ELEVATED", "GREATEST SNOW ON EARTH")),
        StateEntry("VT", "VERMONT", listOf("GREEN MOUNTAIN STATE")),
        StateEntry("VA", "VIRGINIA"),
        StateEntry("WA", "WASHINGTON", listOf("EVERGREEN STATE")),
        StateEntry("WV", "WEST VIRGINIA", listOf("WILD WONDERFUL", "MOUNTAIN STATE")),
        StateEntry("WI", "WISCONSIN", listOf("AMERICAS DAIRYLAND")),
        StateEntry("WY", "WYOMING", listOf("FOREVER WEST")),
    )
}
