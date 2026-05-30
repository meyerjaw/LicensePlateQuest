package com.getmecookies.licenseplatequest.ui.screens.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.getmecookies.licenseplatequest.data.map.MapRepository
import com.getmecookies.licenseplatequest.ui.map.UsMapShapes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * State for the standalone map demo. [shapes] is null until the bundled vector map finishes
 * loading off the main thread.
 */
data class MapDemoUiState(
    val shapes: UsMapShapes? = null,
    val foundCodes: Set<String> = emptySet(),
)

/**
 * Drives a self-contained test of the interactive map (Milestone 5): loads the bundled
 * shapes and toggles "found" state in memory on tap. This lets the map be exercised before
 * the Active Trip View (Milestone 7) wires it to real spotting data.
 */
class MapDemoViewModel(
    mapRepository: MapRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MapDemoUiState())
    val uiState: StateFlow<MapDemoUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val shapes = mapRepository.loadShapes()
            _uiState.update { it.copy(shapes = shapes) }
        }
    }

    fun onToggleState(code: String) {
        _uiState.update { state ->
            val next = state.foundCodes.toMutableSet().apply {
                if (!add(code)) remove(code)
            }
            state.copy(foundCodes = next)
        }
    }

    fun onReset() {
        _uiState.update { it.copy(foundCodes = emptySet()) }
    }
}
