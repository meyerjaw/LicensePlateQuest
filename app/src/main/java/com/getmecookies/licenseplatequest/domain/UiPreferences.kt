package com.getmecookies.licenseplatequest.domain

import android.content.Context

/**
 * Small persistent store for UI-only preferences (SharedPreferences-backed, survives process
 * death and app restarts). Currently remembers which tab the user last viewed on the Active
 * Trip screen, so re-entering a trip restores Map or List.
 */
class UiPreferences(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** Last-selected Active Trip tab, as an enum ordinal (defaults to the first tab, Map). */
    var activeTripTab: Int
        get() = prefs.getInt(KEY_ACTIVE_TRIP_TAB, 0)
        set(value) = prefs.edit().putInt(KEY_ACTIVE_TRIP_TAB, value).apply()

    private companion object {
        const val PREFS = "ui_prefs"
        const val KEY_ACTIVE_TRIP_TAB = "active_trip_tab"
    }
}
