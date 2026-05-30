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
                    state.copy(
                        active = items.firstOrNull { it.status == TripStatus.ACTIVE },
                        inProgress = items.filter { it.status == TripStatus.IN_PROGRESS },
                        completed = items.filter { it.status == TripStatus.COMPLETED },
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
}
