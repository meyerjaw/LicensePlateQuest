package com.getmecookies.licenseplatequest.domain

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Small persistent store for UI-only preferences (SharedPreferences-backed, survives process
 * death and app restarts). Currently remembers which tab the user last viewed on the Active
 * Trip screen, so re-entering a trip restores Map or List.
 */
class UiPreferences(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /**
     * Whether first-run onboarding has been completed or explicitly skipped. Observable so the app
     * root can swap between the onboarding flow and the main app live (e.g. "Restart setup wizard"
     * from Settings re-shows it immediately, not just on next launch).
     */
    private val _onboardingComplete =
        MutableStateFlow(prefs.getBoolean(KEY_ONBOARDING_DONE, false))
    val onboardingComplete: StateFlow<Boolean> = _onboardingComplete.asStateFlow()

    fun setOnboardingComplete(done: Boolean) {
        prefs.edit().putBoolean(KEY_ONBOARDING_DONE, done).apply()
        _onboardingComplete.value = done
    }

    /** The wizard step to resume on if the app is killed mid-onboarding (0-based). */
    var onboardingStep: Int
        get() = prefs.getInt(KEY_ONBOARDING_STEP, 0)
        set(value) = prefs.edit().putInt(KEY_ONBOARDING_STEP, value).apply()

    /** Last-selected Active Trip tab, as an enum ordinal (defaults to the first tab, Map). */
    var activeTripTab: Int
        get() = prefs.getInt(KEY_ACTIVE_TRIP_TAB, 0)
        set(value) = prefs.edit().putInt(KEY_ACTIVE_TRIP_TAB, value).apply()

    /** List tab: whether found states are shown (defaults on; remembered across sessions). */
    var listShowFound: Boolean
        get() = prefs.getBoolean(KEY_LIST_SHOW_FOUND, true)
        set(value) = prefs.edit().putBoolean(KEY_LIST_SHOW_FOUND, value).apply()

    /** List tab: whether unfound states are shown (defaults on; remembered across sessions). */
    var listShowUnfound: Boolean
        get() = prefs.getBoolean(KEY_LIST_SHOW_UNFOUND, true)
        set(value) = prefs.edit().putBoolean(KEY_LIST_SHOW_UNFOUND, value).apply()

    /** Whether the one-time "tap a state to mark it" map hint has been shown/dismissed. */
    var onboardingMapHintSeen: Boolean
        get() = prefs.getBoolean(KEY_MAP_HINT_SEEN, false)
        set(value) = prefs.edit().putBoolean(KEY_MAP_HINT_SEEN, value).apply()

    /** Whether we've ever launched the system POST_NOTIFICATIONS dialog (pre-permission priming). */
    var notificationRequested: Boolean
        get() = prefs.getBoolean(KEY_NOTIF_REQUESTED, false)
        set(value) = prefs.edit().putBoolean(KEY_NOTIF_REQUESTED, value).apply()

    /** After a "Not now" on the primer, skip this many subsequent (non-forced) triggers. */
    var notificationPrimerSnooze: Int
        get() = prefs.getInt(KEY_NOTIF_PRIMER_SNOOZE, 0)
        set(value) = prefs.edit().putInt(KEY_NOTIF_PRIMER_SNOOZE, value).apply()

    private companion object {
        const val PREFS = "ui_prefs"
        const val KEY_ACTIVE_TRIP_TAB = "active_trip_tab"
        const val KEY_LIST_SHOW_FOUND = "list_show_found"
        const val KEY_LIST_SHOW_UNFOUND = "list_show_unfound"
        const val KEY_MAP_HINT_SEEN = "onboarding_map_hint_seen"
        const val KEY_NOTIF_REQUESTED = "notification_requested"
        const val KEY_NOTIF_PRIMER_SNOOZE = "notification_primer_snooze"
        const val KEY_ONBOARDING_DONE = "onboarding_complete"
        const val KEY_ONBOARDING_STEP = "onboarding_step"
    }
}
