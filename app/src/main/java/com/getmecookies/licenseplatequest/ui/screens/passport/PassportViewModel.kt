package com.getmecookies.licenseplatequest.ui.screens.passport

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.getmecookies.licenseplatequest.data.map.MapRepository
import com.getmecookies.licenseplatequest.data.repository.SpottingRepository
import com.getmecookies.licenseplatequest.domain.model.LifetimeState
import com.getmecookies.licenseplatequest.ui.map.UsMapShapes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * The lifetime "Plate Passport" (cross-trip collection): a filled lifetime map, an all-time
 * collected counter, and the list of states caught across every trip with first-spotted dates.
 */
data class PassportUiState(
    val loading: Boolean = true,
    val shapes: UsMapShapes? = null,
    val collected: List<LifetimeState> = emptyList(),
) {
    val collectedCount: Int get() = collected.size
    val remaining: Int get() = (PassportViewModel.TOTAL_STATES - collectedCount).coerceAtLeast(0)
    val foundCodes: Set<String> get() = collected.mapTo(HashSet()) { it.code }
}

class PassportViewModel(
    mapRepository: MapRepository,
    spottingRepository: SpottingRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PassportUiState())
    val uiState: StateFlow<PassportUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val shapes = mapRepository.loadShapes()
            _uiState.update { it.copy(shapes = shapes) }
        }
        viewModelScope.launch {
            spottingRepository.observeLifetimeStates().collect { states ->
                _uiState.update { it.copy(loading = false, collected = states) }
            }
        }
    }

    companion object {
        const val TOTAL_STATES = 50
    }
}
