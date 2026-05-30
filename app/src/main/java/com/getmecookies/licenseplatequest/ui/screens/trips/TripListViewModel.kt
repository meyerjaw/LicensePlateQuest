package com.getmecookies.licenseplatequest.ui.screens.trips

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.getmecookies.licenseplatequest.data.repository.RegionRepository
import com.getmecookies.licenseplatequest.data.repository.TripRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/**
 * ViewModel for the Trip List (Home) screen. For now it exposes a trip count (to confirm
 * creation persists) and the loaded-region count. The full sectioned trip list lands in the
 * Trip List milestone.
 */
class TripListViewModel(
    tripRepository: TripRepository,
    regionRepository: RegionRepository,
) : ViewModel() {

    val tripCount: StateFlow<Int> = tripRepository.observeTripCount()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = 0,
        )

    val regionCount: StateFlow<Int> = regionRepository.observeRegionCount()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = 0,
        )
}
