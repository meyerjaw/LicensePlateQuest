package com.getmecookies.licenseplatequest.domain

import android.content.Context
import java.util.UUID

/**
 * Remembers which trips have already shown their 50/50 celebration, so it fires exactly once
 * per trip (SPEC section 10: re-marking the 50th state must not re-trigger it). Backed by
 * SharedPreferences so it survives process death.
 */
class CelebrationTracker(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun hasCelebratedFifty(tripId: UUID): Boolean =
        prefs.getStringSet(KEY_FIFTY, emptySet())!!.contains(tripId.toString())

    fun markFiftyCelebrated(tripId: UUID) {
        val current = prefs.getStringSet(KEY_FIFTY, emptySet())!!.toMutableSet()
        current.add(tripId.toString())
        prefs.edit().putStringSet(KEY_FIFTY, current).apply()
    }

    private companion object {
        const val PREFS = "celebration_tracker"
        const val KEY_FIFTY = "fifty_celebrated_trip_ids"
    }
}
