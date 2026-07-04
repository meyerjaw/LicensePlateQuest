package com.getmecookies.licenseplatequest.ui.navigation

/**
 * Nested (non-tab) navigation routes. Top-level tabs live in [TopDestination]; these are
 * full screens pushed on top of a tab — e.g. Add Player and New Trip, which deliberately
 * replace dialogs so they can grow in future phases.
 */
object Routes {
    const val ADD_PLAYER = "players/add"
    const val NEW_TRIP = "trips/new"
    const val SETTINGS = "settings"

    /** Experimental camera plate → state recognition spike (see PLATE_RECOGNITION.md). */
    const val SCAN = "scan"

    /** State Detail, parameterized by 2-letter state code. */
    const val STATE_DETAIL = "state/{code}"
    fun stateDetail(code: String) = "state/$code"

    /** Celebration screen: trip id + mode (FIFTY_FIFTY or MANUAL_END). */
    const val CELEBRATION = "celebration/{tripId}/{mode}"
    fun celebration(tripId: String, mode: String) = "celebration/$tripId/$mode"

    /** Manage/edit-trip screen for a trip, parameterized by trip id (playtest #14). */
    const val EDIT_TRIP = "trips/{tripId}/edit"
    fun editTrip(tripId: String) = "trips/$tripId/edit"

    /** savedStateHandle key: id of a player created via the shared Add Player screen. */
    const val RESULT_NEW_PLAYER_ID = "new_player_id"
}
