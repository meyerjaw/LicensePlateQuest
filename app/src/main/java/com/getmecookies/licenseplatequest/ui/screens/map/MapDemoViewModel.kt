package com.getmecookies.licenseplatequest.ui.screens.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.getmecookies.licenseplatequest.data.map.MapRepository
import com.getmecookies.licenseplatequest.data.repository.SpottingRepository
import com.getmecookies.licenseplatequest.ui.map.UsMapShapes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * State for the map screen. [shapes] is null until the bundled vector map finishes loading
 * off the main thread; [foundCodes] reflects the active trip's spottings.
 */
data class MapDemoUiState(
    val shapes: UsMapShapes? = null,
    val foundCodes: Set<String> = emptySet(),
)

/**
 * Drives the interactive map. Loads the bundled shapes once, then mirrors the *active trip's*
 * found states (Milestone 6) so colored fills reflect real spottings. Tapping a state is
 * handled by the screen (navigates to State Detail) rather than toggled here — this VM is
 * read-only with respect to spottings. Becomes the Active Trip View's map in Milestone 7.
 */
class MapDemoViewModel(
    mapRepository: MapRepository,
    spottingRepository: SpottingRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MapDemoUiState())
    val uiState: StateFlow<MapDemoUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val shapes = mapRepository.loadShapes()
            _uiState.update { it.copy(shapes = shapes) }
        }
        viewModelScope.launch {
            spottingRepository.observeFoundCodesForActiveTrip().collect { codes ->
                _uiState.update { it.copy(foundCodes = codes) }
            }
        }
    }
}
