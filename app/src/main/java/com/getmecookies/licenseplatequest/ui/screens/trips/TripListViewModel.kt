package com.getmecookies.licenseplatequest.ui.screens.trips

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.getmecookies.licenseplatequest.data.repository.RegionRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/**
 * ViewModel for the Trip List (Home) screen. In Foundation it exposes only the count of
 * loaded reference regions — enough to confirm the database + bundled-data seeding work
 * end-to-end. Trip data is layered on in the Trip Creation / Trip List milestones.
 */
class TripListViewModel(
    regionRepository: RegionRepository,
) : ViewModel() {

    val regionCount: StateFlow<Int> = regionRepository.observeRegionCount()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = 0,
        )
}
