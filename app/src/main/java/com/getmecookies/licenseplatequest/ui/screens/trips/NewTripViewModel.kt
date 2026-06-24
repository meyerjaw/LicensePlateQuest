package com.getmecookies.licenseplatequest.ui.screens.trips

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.getmecookies.licenseplatequest.data.repository.PlayerRepository
import com.getmecookies.licenseplatequest.data.repository.RegionRepository
import com.getmecookies.licenseplatequest.data.repository.SettingsRepository
import com.getmecookies.licenseplatequest.data.repository.TripRepository
import com.getmecookies.licenseplatequest.domain.Analytics
import com.getmecookies.licenseplatequest.domain.NoOpAnalytics
import com.getmecookies.licenseplatequest.domain.model.TripStop
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID

/**
 * ViewModel for the full-screen New Trip form (SPEC sections 6/7). Owns prefill, validation,
 * player selection (incl. inline quick-add), and the transactional create call. On success
 * it flips [saved], which the screen observes to navigate to the (future) active trip view.
 */
class NewTripViewModel(
    private val tripRepository: TripRepository,
    regionRepository: RegionRepository,
    private val playerRepository: PlayerRepository,
    settingsRepository: SettingsRepository,
    private val analytics: Analytics = NoOpAnalytics,
) : ViewModel() {

    private val _uiState = MutableStateFlow(NewTripUiState())
    val uiState: StateFlow<NewTripUiState> = _uiState.asStateFlow()

    private val _saved = MutableStateFlow(false)
    val saved: StateFlow<Boolean> = _saved.asStateFlow()

    init {
        // Pre-fill the "From" field from the saved home location (a suggestion; the user can edit
        // or clear it, and clearing won't re-populate). Playtest note #8.
        settingsRepository.home.value?.let { home ->
            _uiState.update { state ->
                val stops = state.stops.toMutableList()
                stops[0] = StopDraft(city = home.city, regionId = home.regionId)
                state.copy(stops = stops).withPrefilledName()
            }
        }
        viewModelScope.launch {
            regionRepository.observeRegionOptions().collect { options ->
                _uiState.update { it.copy(regionOptions = options).withPrefilledName() }
            }
        }
        viewModelScope.launch {
            playerRepository.observePlayers().collect { players ->
                _uiState.update { it.copy(allPlayers = players) }
            }
        }
    }

    // --- Field edits -------------------------------------------------------

    fun onNameChange(value: String) {
        _uiState.update { it.copy(name = value, nameManuallyEdited = true) }
    }

    /** Clear the trip name; stays empty (auto-prefill won't re-populate) for the user to retype. */
    fun onClearName() {
        _uiState.update { it.copy(name = "", nameManuallyEdited = true) }
    }

    fun onStopCityChange(index: Int, value: String) = updateStop(index) { it.copy(city = value) }

    fun onStopRegionSelected(index: Int, id: UUID) = updateStop(index) { it.copy(regionId = id) }

    /** Append a new empty pit stop at the end (it becomes the new destination slot to fill). */
    fun onAddStop() {
        _uiState.update { it.copy(stops = it.stops + StopDraft()).withPrefilledName() }
    }

    /** Remove a stop. No-op below the two-stop minimum (a trip always has a start + destination). */
    fun onRemoveStop(index: Int) {
        _uiState.update { state ->
            if (state.stops.size <= 2 || index !in state.stops.indices) {
                state
            } else {
                state.copy(stops = state.stops.filterIndexed { i, _ -> i != index }).withPrefilledName()
            }
        }
    }

    fun onMoveStopUp(index: Int) = moveStop(index, index - 1)

    fun onMoveStopDown(index: Int) = moveStop(index, index + 1)

    private fun updateStop(index: Int, transform: (StopDraft) -> StopDraft) {
        _uiState.update { state ->
            if (index !in state.stops.indices) {
                state
            } else {
                val stops = state.stops.toMutableList()
                stops[index] = transform(stops[index])
                state.copy(stops = stops).withPrefilledName()
            }
        }
    }

    private fun moveStop(from: Int, to: Int) {
        _uiState.update { state ->
            if (from !in state.stops.indices || to !in state.stops.indices) {
                state
            } else {
                val stops = state.stops.toMutableList()
                stops.add(to, stops.removeAt(from))
                state.copy(stops = stops).withPrefilledName()
            }
        }
    }

    fun onStartDateChange(date: LocalDate) {
        _uiState.update { state ->
            // Keep end >= start: if the new start passes the end date, push the end forward.
            val end = state.endDate?.let { if (it.isBefore(date)) date else it }
            state.copy(startDate = date, endDate = end).withPrefilledName()
        }
    }

    fun onEndDateChange(date: LocalDate) {
        _uiState.update { state ->
            val clamped = if (date.isBefore(state.startDate)) state.startDate else date
            state.copy(endDate = clamped)
        }
    }

    fun onClearEndDate() {
        _uiState.update { it.copy(endDate = null) }
    }

    // --- Players -----------------------------------------------------------

    fun onTogglePlayer(id: UUID) {
        _uiState.update { state ->
            val next = state.selectedPlayerIds.toMutableSet().apply {
                if (!add(id)) remove(id)
            }
            state.copy(selectedPlayerIds = next)
        }
    }

    /**
     * Auto-select a player created via the shared full-screen Add Player flow (SPEC section 6
     * "+ Add new player" quick-add). The roster list itself updates through the observed
     * players flow; here we just mark the returned id selected.
     */
    fun onExternalPlayerAdded(id: UUID) {
        _uiState.update { it.copy(selectedPlayerIds = it.selectedPlayerIds + id) }
    }

    // --- Save --------------------------------------------------------------

    fun onSave() {
        val state = _uiState.value
        if (state.saving) return
        if (!state.isValid) {
            _uiState.update { it.copy(showErrors = true) }
            return
        }
        _uiState.update { it.copy(saving = true) }
        viewModelScope.launch {
            tripRepository.createTrip(
                name = state.name,
                stops = state.stops.map { TripStop(regionId = it.regionId!!, city = it.city) },
                startDate = state.startDate,
                endDate = state.endDate,
                playerIds = state.selectedPlayerIds.toList(),
            )
            analytics.event(
                "trip_created",
                mapOf(
                    "player_count" to state.selectedPlayerIds.size,
                    "stop_count" to state.stops.size,
                    "has_end_date" to (state.endDate != null),
                ),
            )
            _saved.value = true
        }
    }

    /**
     * Recompute the auto-prefilled name unless the user has taken over the field. Format:
     * "Start to … to Destination - Month Year", using each stop's city (or its state code), with
     * the destination state appended. A single known stop stays open as "X to ".
     */
    private fun NewTripUiState.withPrefilledName(): NewTripUiState {
        if (nameManuallyEdited) return this
        val lastIndex = stops.lastIndex
        val labels = stops.mapIndexedNotNull { index, stop ->
            val city = stop.city.trim()
            val code = regionCode(stop.regionId)
            when {
                // The final stop carries its state too, e.g. "Cincinnati, OH".
                city.isNotBlank() && index == lastIndex && code != null -> "$city, $code"
                city.isNotBlank() -> city
                code != null -> code
                else -> null
            }
        }
        val monthYear = startDate.format(MONTH_YEAR)
        val prefilled = when {
            labels.size >= 2 -> labels.joinToString(" to ") + " - $monthYear"
            // One known stop (e.g. home prefill): leave it open as "X to ".
            labels.size == 1 -> "${labels.first()} to "
            else -> ""
        }
        return copy(name = prefilled)
    }

    private companion object {
        val MONTH_YEAR: DateTimeFormatter =
            DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())
    }
}
