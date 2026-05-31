package com.getmecookies.licenseplatequest.ui.screens.activetrip

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.getmecookies.licenseplatequest.data.map.MapRepository
import com.getmecookies.licenseplatequest.data.repository.SpottingRepository
import com.getmecookies.licenseplatequest.data.repository.TripRepository
import com.getmecookies.licenseplatequest.domain.CelebrationTracker
import com.getmecookies.licenseplatequest.domain.model.FoundState
import com.getmecookies.licenseplatequest.ui.map.UsMapShapes
import com.getmecookies.licenseplatequest.ui.screens.celebration.CelebrationMode
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
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
    /** One-shot: navigate to a celebration for (tripId, mode). Cleared via [ActiveTripViewModel.onCelebrationConsumed]. */
    val celebration: Celebration? = null,
) {
    val foundCount: Int get() = foundCodes.size
}

data class Celebration(val tripId: UUID, val mode: CelebrationMode)

/**
 * Drives the Active Trip View (SPEC section 6). Combines the active trip, its found states, and
 * the chosen sort order into one UI state; the bundled map shapes load once on the side.
 *
 * Celebrations (SPEC section 6/8): when the found count rises it fires a per-state confetti
 * tick; reaching 50 fires the 50/50 celebration exactly once per trip (tracked via
 * [CelebrationTracker] so re-marking can't replay it). Ending a trip routes through the
 * manual-end celebration, which finalizes the trip.
 */
class ActiveTripViewModel(
    mapRepository: MapRepository,
    private val tripRepository: TripRepository,
    spottingRepository: SpottingRepository,
    private val celebrationTracker: CelebrationTracker,
) : ViewModel() {

    private val sort = MutableStateFlow(FoundSort.ORDER_FOUND)
    private val _uiState = MutableStateFlow(ActiveTripUiState())
    val uiState: StateFlow<ActiveTripUiState> = _uiState.asStateFlow()

    /**
     * One-shot per-state confetti signals. A Channel (not state) so each burst fires exactly
     * once and is never replayed when the screen re-enters composition (e.g. returning from
     * State Detail without marking anything).
     */
    private val _confettiEvents = Channel<Unit>(Channel.BUFFERED)
    val confettiEvents: Flow<Unit> = _confettiEvents.receiveAsFlow()

    // Track the previous found count per trip so we only react to genuine increases.
    private var prevTripId: UUID? = null
    private var prevCount: Int? = null

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
                detectCelebrations(trip?.id, found.size)
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

    /** Fire per-state confetti and the once-per-trip 50/50 celebration on real count increases. */
    private fun detectCelebrations(tripId: UUID?, count: Int) {
        // Reset the baseline when the active trip changes (avoids cross-trip false positives).
        if (tripId != prevTripId) {
            prevTripId = tripId
            prevCount = count
            return
        }
        val previous = prevCount
        prevCount = count
        if (tripId == null || previous == null || count <= previous) return

        if (count >= TOTAL_STATES && !celebrationTracker.hasCelebratedFifty(tripId)) {
            celebrationTracker.markFiftyCelebrated(tripId)
            _uiState.update {
                it.copy(celebration = Celebration(tripId, CelebrationMode.FIFTY_FIFTY))
            }
        } else {
            _confettiEvents.trySend(Unit)
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

    /**
     * Confirming "End trip" routes to the manual-end celebration; the trip is finalized there
     * (so the celebration can still read its stats while the trip is intact).
     */
    fun onConfirmEndTrip() {
        val tripId = _uiState.value.tripId
        _uiState.update {
            it.copy(
                showEndDialog = false,
                celebration = tripId?.let { id -> Celebration(id, CelebrationMode.MANUAL_END) },
            )
        }
    }

    fun onCelebrationConsumed() {
        _uiState.update { it.copy(celebration = null) }
    }

    private companion object {
        const val TOTAL_STATES = 50
    }
}
