package com.getmecookies.licenseplatequest.ui.screens.trips

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.getmecookies.licenseplatequest.data.repository.PlayerRepository
import com.getmecookies.licenseplatequest.data.repository.RegionRepository
import com.getmecookies.licenseplatequest.data.repository.SettingsRepository
import com.getmecookies.licenseplatequest.data.repository.TripRepository
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
) : ViewModel() {

    private val _uiState = MutableStateFlow(NewTripUiState())
    val uiState: StateFlow<NewTripUiState> = _uiState.asStateFlow()

    private val _saved = MutableStateFlow(false)
    val saved: StateFlow<Boolean> = _saved.asStateFlow()

    init {
        // Pre-fill the "From" field from the saved home location (a suggestion; the user can edit
        // or clear it, and clearing won't re-populate). Playtest note #8.
        settingsRepository.home.value?.let { home ->
            _uiState.update {
                it.copy(originCity = home.city, originRegionId = home.regionId).withPrefilledName()
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

    fun onOriginCityChange(value: String) {
        _uiState.update { it.copy(originCity = value).withPrefilledName() }
    }

    fun onDestinationCityChange(value: String) {
        _uiState.update { it.copy(destinationCity = value).withPrefilledName() }
    }

    fun onOriginRegionSelected(id: UUID) {
        _uiState.update { it.copy(originRegionId = id).withPrefilledName() }
    }

    fun onDestinationRegionSelected(id: UUID) {
        _uiState.update { it.copy(destinationRegionId = id).withPrefilledName() }
    }

    /** Quick-clear both subfields of a section at once (playtest note #9). */
    fun onClearOrigin() {
        _uiState.update { it.copy(originCity = "", originRegionId = null).withPrefilledName() }
    }

    fun onClearDestination() {
        _uiState.update { it.copy(destinationCity = "", destinationRegionId = null).withPrefilledName() }
    }

    fun onStartDateChange(date: LocalDate) {
        _uiState.update { it.copy(startDate = date).withPrefilledName() }
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
                originCity = state.originCity,
                originRegionId = state.originRegionId!!,
                destinationCity = state.destinationCity,
                destinationRegionId = state.destinationRegionId!!,
                startDate = state.startDate,
                playerIds = state.selectedPlayerIds.toList(),
            )
            _saved.value = true
        }
    }

    /**
     * Recompute the auto-prefilled name unless the user has taken over the field. Format:
     * "Origin to Destination, Month Year", filling in whatever pieces are known so far.
     */
    private fun NewTripUiState.withPrefilledName(): NewTripUiState {
        if (nameManuallyEdited) return this
        val origin = originCity.trim().ifBlank { originRegion?.code ?: "" }
        // Destination includes its state, e.g. "Cincinnati, OH".
        val destCity = destinationCity.trim()
        val destCode = destinationRegion?.code
        val destination = when {
            destCity.isNotBlank() && !destCode.isNullOrBlank() -> "$destCity, $destCode"
            destCity.isNotBlank() -> destCity
            !destCode.isNullOrBlank() -> destCode
            else -> ""
        }
        val monthYear = startDate.format(MONTH_YEAR)
        val prefilled = when {
            origin.isNotBlank() && destination.isNotBlank() -> "$origin to $destination - $monthYear"
            // Origin only (e.g. pre-filled from home): leave it open as "X to " so it completes to
            // the full name the moment a destination is filled in (playtest follow-up).
            origin.isNotBlank() -> "$origin to "
            destination.isNotBlank() -> "Trip to $destination - $monthYear"
            else -> ""
        }
        return copy(name = prefilled)
    }

    private companion object {
        val MONTH_YEAR: DateTimeFormatter =
            DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())
    }
}
