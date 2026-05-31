package com.getmecookies.licenseplatequest.ui.screens.activetrip

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.getmecookies.licenseplatequest.data.map.MapRepository
import com.getmecookies.licenseplatequest.data.repository.SpottingRepository
import com.getmecookies.licenseplatequest.data.repository.TripRepository
import com.getmecookies.licenseplatequest.domain.model.FoundState
import com.getmecookies.licenseplatequest.ui.map.UsMapShapes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

/** How the found-states bottom sheet is ordered (SPEC section 6). */
enum class FoundSort { ORDER_FOUND, ALPHABETICAL }

data class ActiveTripUiState(
    val loading: Boolean = true,
    val tripId: UUID? = null,
    val tripName: String = "",
    val shapes: UsMapShapes? = null,
    val foundCodes: Set<String> = emptySet(),
    val foundStates: List<FoundState> = emptyList(),
    val sort: FoundSort = FoundSort.ORDER_FOUND,
    val showEndDialog: Boolean = false,
) {
    val foundCount: Int get() = foundCodes.size
}

/**
 * Drives the Active Trip View (SPEC section 6). Combines the active trip, its found states,
 * and the chosen sort order into one UI state; the bundled map shapes load once on the side.
 * Owns the End-trip flow. The map's tap-to-detail is handled by the screen/nav, so this VM is
 * read-only with respect to spottings (marking happens on State Detail).
 */
class ActiveTripViewModel(
    mapRepository: MapRepository,
    private val tripRepository: TripRepository,
    spottingRepository: SpottingRepository,
) : ViewModel() {

    private val sort = MutableStateFlow(FoundSort.ORDER_FOUND)
    private val _uiState = MutableStateFlow(ActiveTripUiState())
    val uiState: StateFlow<ActiveTripUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val shapes = mapRepository.loadShapes()
            _uiState.update { it.copy(shapes = shapes) }
        }
        viewModelScope.launch {
            combine(
                tripRepository.observeActiveTrip(),
                spottingRepository.observeFoundStatesForActiveTrip(),
                sort,
            ) { trip, found, sortMode ->
                Triple(trip, found, sortMode)
            }.collect { (trip, found, sortMode) ->
                val ordered = when (sortMode) {
                    FoundSort.ORDER_FOUND -> found // already newest-first from the query
                    FoundSort.ALPHABETICAL -> found.sortedBy { it.name }
                }
                _uiState.update {
                    it.copy(
                        loading = false,
                        tripId = trip?.id,
                        tripName = trip?.name ?: "",
                        foundCodes = found.map { f -> f.code }.toSet(),
                        foundStates = ordered,
                        sort = sortMode,
                    )
                }
            }
        }
    }

    fun onSortChange(newSort: FoundSort) {
        sort.value = newSort
    }

    fun onEndTripClick() {
        _uiState.update { it.copy(showEndDialog = true) }
    }

    fun onDismissEndDialog() {
        _uiState.update { it.copy(showEndDialog = false) }
    }

    fun onConfirmEndTrip() {
        val tripId = _uiState.value.tripId
        _uiState.update { it.copy(showEndDialog = false) }
        if (tripId != null) {
            viewModelScope.launch { tripRepository.endTrip(tripId) }
        }
    }
}
