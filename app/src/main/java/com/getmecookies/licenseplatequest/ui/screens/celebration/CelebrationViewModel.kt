package com.getmecookies.licenseplatequest.ui.screens.celebration

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.getmecookies.licenseplatequest.data.repository.CelebrationRepository
import com.getmecookies.licenseplatequest.data.repository.TripRepository
import com.getmecookies.licenseplatequest.domain.model.CelebrationStats
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

/** Which celebration is showing (SPEC section 6). */
enum class CelebrationMode { FIFTY_FIFTY, MANUAL_END }

data class CelebrationUiState(
    val loading: Boolean = true,
    val mode: CelebrationMode = CelebrationMode.FIFTY_FIFTY,
    val stats: CelebrationStats? = null,
    /** Set true after a manual-end trip is finalized, so the screen can exit to the list. */
    val finished: Boolean = false,
)

/**
 * Loads celebration stats for a trip and, for a manual end, finalizes the trip (SPEC: a trip
 * becomes COMPLETED only on manual end). The 50/50 celebration leaves the trip active.
 */
class CelebrationViewModel(
    savedStateHandle: SavedStateHandle,
    private val celebrationRepository: CelebrationRepository,
    private val tripRepository: TripRepository,
) : ViewModel() {

    private val tripId: UUID = UUID.fromString(checkNotNull(savedStateHandle[ARG_TRIP_ID]))
    private val mode: CelebrationMode = CelebrationMode.valueOf(
        checkNotNull(savedStateHandle[ARG_MODE]),
    )

    private val _uiState = MutableStateFlow(CelebrationUiState(mode = mode))
    val uiState: StateFlow<CelebrationUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val stats = celebrationRepository.getStats(tripId)
            _uiState.update { it.copy(loading = false, stats = stats) }
        }
    }

    /**
     * Finalize a manual-end celebration: end the trip, then flag the screen to exit. For a
     * 50/50 celebration this isn't called — the screen just dismisses (trip stays active).
     */
    fun onFinishManualEnd() {
        viewModelScope.launch {
            tripRepository.endTrip(tripId)
            _uiState.update { it.copy(finished = true) }
        }
    }

    companion object {
        const val ARG_TRIP_ID = "tripId"
        const val ARG_MODE = "mode"
    }
}
