package com.getmecookies.licenseplatequest.ui.screens.trips

import com.getmecookies.licenseplatequest.domain.model.TripListItem

/**
 * UI state for the Trip List (SPEC section 6). Trips are pre-grouped into the three spec
 * sections so the screen stays a thin renderer.
 *
 * [deleteTarget] holds a trip pending a long-press delete confirmation dialog.
 * [pendingDelete] holds a trip that was swiped away and is hidden pending an undo snackbar;
 * its real deletion is committed only when the snackbar dismisses without undo.
 */
data class TripListUiState(
    val active: TripListItem? = null,
    val inProgress: List<TripListItem> = emptyList(),
    val completed: List<TripListItem> = emptyList(),
    val loading: Boolean = true,
    val deleteTarget: TripListItem? = null,
    val pendingDelete: TripListItem? = null,
) {
    val isEmpty: Boolean
        get() = active == null && inProgress.isEmpty() && completed.isEmpty()
}
