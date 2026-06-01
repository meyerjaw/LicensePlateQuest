package com.getmecookies.licenseplatequest.ui.screens.trips

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.getmecookies.licenseplatequest.data.repository.TripRepository
import com.getmecookies.licenseplatequest.domain.model.TripListItem
import com.getmecookies.licenseplatequest.domain.model.TripStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * ViewModel for the Trip List (SPEC section 6). Observes all trips and groups them into the
 * Active / In Progress / Completed sections, and owns selection (activate) and delete flows.
 */
class TripListViewModel(
    private val tripRepository: TripRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TripListUiState())
    val uiState: StateFlow<TripListUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            tripRepository.observeTripListItems().collect { items ->
                _uiState.update { state ->
                    // Hide a swipe-pending trip from the sections until its undo window closes.
                    val pendingId = state.pendingDelete?.id
                    val visible = items.filter { it.id != pendingId }
                    state.copy(
                        active = visible.firstOrNull { it.status == TripStatus.ACTIVE },
                        inProgress = visible.filter { it.status == TripStatus.IN_PROGRESS },
                        completed = visible.filter { it.status == TripStatus.COMPLETED },
                        loading = false,
                    )
                }
            }
        }
    }

    /** Selecting a trip makes it the active one (SPEC section 5 navigation). */
    fun onSelectTrip(id: UUID) {
        viewModelScope.launch { tripRepository.setActiveTrip(id) }
    }

    // --- Delete flow -------------------------------------------------------

    fun onDeleteRequest(item: TripListItem) {
        _uiState.update { it.copy(deleteTarget = item) }
    }

    fun onDismissDelete() {
        _uiState.update { it.copy(deleteTarget = null) }
    }

    fun onConfirmDelete() {
        val target = _uiState.value.deleteTarget ?: return
        viewModelScope.launch {
            tripRepository.deleteTrip(target.id)
            _uiState.update { it.copy(deleteTarget = null) }
        }
    }

    // --- Swipe-to-delete with undo ----------------------------------------

    /**
     * Swiping a row hides it and starts the undo window. The real delete is deferred until
     * [onPendingDeleteCommit]; [onUndoDelete] cancels it. Re-grouping happens via the observed
     * flow once [pendingDelete] is set, so the row vanishes immediately.
     */
    fun onSwipeDelete(item: TripListItem) {
        _uiState.update { state ->
            val visible = listOfNotNull(state.active) + state.inProgress + state.completed
            val remaining = visible.filter { it.id != item.id }
            state.copy(
                pendingDelete = item,
                active = remaining.firstOrNull { it.status == TripStatus.ACTIVE },
                inProgress = remaining.filter { it.status == TripStatus.IN_PROGRESS },
                completed = remaining.filter { it.status == TripStatus.COMPLETED },
            )
        }
    }

    /** Undo a swipe: the trip reappears (the observed flow re-includes it) and nothing is deleted. */
    fun onUndoDelete() {
        val restored = _uiState.value.pendingDelete ?: return
        _uiState.update { state ->
            val visible = listOfNotNull(state.active) + state.inProgress + state.completed + restored
            state.copy(
                pendingDelete = null,
                active = visible.firstOrNull { it.status == TripStatus.ACTIVE },
                inProgress = visible.filter { it.status == TripStatus.IN_PROGRESS },
                completed = visible.filter { it.status == TripStatus.COMPLETED },
            )
        }
    }

    /** Commit the deferred delete once the undo window has passed without an undo. */
    fun onPendingDeleteCommit() {
        val target = _uiState.value.pendingDelete ?: return
        viewModelScope.launch {
            tripRepository.deleteTrip(target.id)
            _uiState.update { it.copy(pendingDelete = null) }
        }
    }
}
