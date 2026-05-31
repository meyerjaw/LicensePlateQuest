package com.getmecookies.licenseplatequest.ui.screens.trips

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.getmecookies.licenseplatequest.data.repository.TripRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * Decides what the Trips tab shows (SPEC section 5): the Active Trip View when a trip is
 * active, otherwise the Trip List. [hasActiveTrip] is null while still loading so the gateway
 * can avoid flashing the wrong screen on first composition.
 */
class TripsTabViewModel(
    tripRepository: TripRepository,
) : ViewModel() {

    val hasActiveTrip: StateFlow<Boolean?> = tripRepository.observeActiveTrip()
        .map { it != null }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null,
        )
}
