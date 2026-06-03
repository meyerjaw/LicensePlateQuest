package com.getmecookies.licenseplatequest.ui.screens.trips

import com.getmecookies.licenseplatequest.domain.model.TripListItem

/**
 * UI state for the Trip List (SPEC section 6). Trips are pre-grouped into the three spec
 * sections so the screen stays a thin renderer.
 *
 * [deleteTarget] holds a trip pending a long-press delete confirmation dialog. Swipe-delete is
 * handled in-place by the shared `SwipeToDeleteRow` (its own undo window), so it needs no state
 * here — the commit just deletes and the observed flow drops the row.
 */
data class TripListUiState(
    val active: TripListItem? = null,
    val inProgress: List<TripListItem> = emptyList(),
    val completed: List<TripListItem> = emptyList(),
    val loading: Boolean = true,
    val deleteTarget: TripListItem? = null,
) {
    val isEmpty: Boolean
        get() = active == null && inProgress.isEmpty() && completed.isEmpty()
}
