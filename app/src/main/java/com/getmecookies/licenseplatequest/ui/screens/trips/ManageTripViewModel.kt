package com.getmecookies.licenseplatequest.ui.screens.trips

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.getmecookies.licenseplatequest.data.repository.PlayerRepository
import com.getmecookies.licenseplatequest.data.repository.RegionRepository
import com.getmecookies.licenseplatequest.data.repository.TripRepository
import com.getmecookies.licenseplatequest.domain.model.TripStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.UUID

/**
 * ViewModel for the Manage trip (edit) screen (playtest #14). Loads the trip and its players
 * once, prefills the form, and stages all edits until [onSave] — which writes the trip fields via
 * [TripRepository.updateTrip] and reconciles players against the loaded set. [isDirty] backs the
 * unsaved-changes warning.
 */
class ManageTripViewModel(
    savedStateHandle: SavedStateHandle,
    private val tripRepository: TripRepository,
    regionRepository: RegionRepository,
    private val playerRepository: PlayerRepository,
) : ViewModel() {

    private val tripId: UUID = UUID.fromString(
        checkNotNull(savedStateHandle[ARG_TRIP_ID]) { "ManageTrip requires a '$ARG_TRIP_ID' argument" },
    )

    private val _uiState = MutableStateFlow(ManageTripUiState())
    val uiState: StateFlow<ManageTripUiState> = _uiState.asStateFlow()

    private val _saved = MutableStateFlow(false)
    val saved: StateFlow<Boolean> = _saved.asStateFlow()

    /** The trip as loaded — used for unsaved-changes detection and player diffing on save. */
    private var original: Snapshot? = null

    /** The loaded trip's status, so we only prompt to end a trip that isn't already completed. */
    private var loadedStatus: TripStatus? = null

    private data class Snapshot(
        val name: String,
        val originCity: String,
        val originRegionId: UUID?,
        val destinationCity: String,
        val destinationRegionId: UUID?,
        val startDate: LocalDate,
        val endDate: LocalDate?,
        val playerIds: Set<UUID>,
    )

    init {
        viewModelScope.launch {
            regionRepository.observeRegionOptions().collect { options ->
                _uiState.update { it.copy(regionOptions = options) }
            }
        }
        viewModelScope.launch {
            playerRepository.observePlayers().collect { players ->
                _uiState.update { it.copy(allPlayers = players) }
            }
        }
        viewModelScope.launch {
            val trip = tripRepository.getTrip(tripId)
            if (trip == null) {
                _uiState.update { it.copy(loading = false) }
                return@launch
            }
            val playerIds = tripRepository.observePlayerIdsForTrip(tripId).first().toSet()
            loadedStatus = trip.status
            original = Snapshot(
                name = trip.name,
                originCity = trip.originCity,
                originRegionId = trip.originRegionId,
                destinationCity = trip.destinationCity,
                destinationRegionId = trip.destinationRegionId,
                startDate = trip.startDate,
                endDate = trip.endDate,
                playerIds = playerIds,
            )
            _uiState.update {
                it.copy(
                    loading = false,
                    name = trip.name,
                    originCity = trip.originCity,
                    originRegionId = trip.originRegionId,
                    destinationCity = trip.destinationCity,
                    destinationRegionId = trip.destinationRegionId,
                    startDate = trip.startDate,
                    endDate = trip.endDate,
                    selectedPlayerIds = playerIds,
                )
            }
        }
    }

    fun onNameChange(value: String) = _uiState.update { it.copy(name = value) }

    fun onClearName() = _uiState.update { it.copy(name = "") }

    fun onOriginCityChange(value: String) = _uiState.update { it.copy(originCity = value) }

    fun onDestinationCityChange(value: String) = _uiState.update { it.copy(destinationCity = value) }

    fun onOriginRegionSelected(id: UUID) = _uiState.update { it.copy(originRegionId = id) }

    fun onDestinationRegionSelected(id: UUID) = _uiState.update { it.copy(destinationRegionId = id) }

    fun onClearOrigin() = _uiState.update { it.copy(originCity = "", originRegionId = null) }

    fun onClearDestination() = _uiState.update { it.copy(destinationCity = "", destinationRegionId = null) }

    fun onStartDateChange(date: LocalDate) = _uiState.update { state ->
        // Keep end ≥ start: if start moves past the end, push the end forward too.
        val end = state.endDate?.let { if (it.isBefore(date)) date else it }
        state.copy(startDate = date, endDate = end)
    }

    fun onEndDateChange(date: LocalDate) = _uiState.update { state ->
        val clamped = if (date.isBefore(state.startDate)) state.startDate else date
        state.copy(endDate = clamped)
    }

    fun onClearEndDate() = _uiState.update { it.copy(endDate = null) }

    fun onTogglePlayer(id: UUID) = _uiState.update { state ->
        val next = state.selectedPlayerIds.toMutableSet().apply { if (!add(id)) remove(id) }
        state.copy(selectedPlayerIds = next)
    }

    fun onExternalPlayerAdded(id: UUID) =
        _uiState.update { it.copy(selectedPlayerIds = it.selectedPlayerIds + id) }

    /** True once the staged form differs from the loaded trip — drives the discard-changes prompt. */
    fun isDirty(): Boolean {
        val o = original ?: return false
        val s = _uiState.value
        return s.name != o.name ||
            s.originCity != o.originCity ||
            s.originRegionId != o.originRegionId ||
            s.destinationCity != o.destinationCity ||
            s.destinationRegionId != o.destinationRegionId ||
            s.startDate != o.startDate ||
            s.endDate != o.endDate ||
            s.selectedPlayerIds != o.playerIds
    }

    fun onSave() {
        val state = _uiState.value
        if (state.saving) return
        if (!state.isValid) {
            _uiState.update { it.copy(showErrors = true) }
            return
        }
        // Saving a non-completed trip with a past end date is ambiguous — ask whether to end it now.
        val endDate = state.endDate
        if (endDate != null && endDate.isBefore(LocalDate.now()) && loadedStatus != TripStatus.COMPLETED) {
            _uiState.update { it.copy(showEndTripPrompt = true) }
            return
        }
        persist(endNow = false)
    }

    /** User chose "End trip" on the past-end-date prompt: save the edits and finalize the trip. */
    fun onConfirmEndTripNow() {
        _uiState.update { it.copy(showEndTripPrompt = false) }
        persist(endNow = true)
    }

    /** User chose "Keep active": save the edits but leave the trip running (it shows as overdue). */
    fun onKeepActive() {
        _uiState.update { it.copy(showEndTripPrompt = false) }
        persist(endNow = false)
    }

    /** Dismiss the prompt without saving (back to editing). */
    fun onDismissEndTripPrompt() {
        _uiState.update { it.copy(showEndTripPrompt = false) }
    }

    private fun persist(endNow: Boolean) {
        val state = _uiState.value
        if (state.saving) return
        _uiState.update { it.copy(saving = true) }
        viewModelScope.launch {
            tripRepository.updateTrip(
                tripId = tripId,
                name = state.name,
                originCity = state.originCity,
                originRegionId = state.originRegionId!!,
                destinationCity = state.destinationCity,
                destinationRegionId = state.destinationRegionId!!,
                startDate = state.startDate,
                endDate = state.endDate,
            )
            // Reconcile players against the set we loaded with.
            val originalIds = original?.playerIds ?: emptySet()
            val target = state.selectedPlayerIds
            (target - originalIds).forEach { tripRepository.addPlayerToTrip(tripId, it) }
            (originalIds - target).forEach { tripRepository.removePlayerFromTrip(tripId, it) }
            if (endNow) tripRepository.endTrip(tripId)
            _saved.value = true
        }
    }

    companion object {
        const val ARG_TRIP_ID = "tripId"
    }
}
