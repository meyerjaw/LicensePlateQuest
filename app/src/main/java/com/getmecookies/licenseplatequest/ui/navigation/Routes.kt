package com.getmecookies.licenseplatequest.ui.navigation

/**
 * Nested (non-tab) navigation routes. Top-level tabs live in [TopDestination]; these are
 * full screens pushed on top of a tab — e.g. Add Player and New Trip, which deliberately
 * replace dialogs so they can grow in future phases.
 */
object Routes {
    const val ADD_PLAYER = "players/add"
    const val NEW_TRIP = "trips/new"

    /** State Detail, parameterized by 2-letter state code. */
    const val STATE_DETAIL = "state/{code}"
    fun stateDetail(code: String) = "state/$code"

    /** savedStateHandle key: id of a player created via the shared Add Player screen. */
    const val RESULT_NEW_PLAYER_ID = "new_player_id"
}
