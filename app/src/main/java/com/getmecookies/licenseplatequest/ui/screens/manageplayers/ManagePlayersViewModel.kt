package com.getmecookies.licenseplatequest.ui.screens.manageplayers

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.getmecookies.licenseplatequest.data.repository.PlayerRepository
import com.getmecookies.licenseplatequest.data.repository.TripRepository
import com.getmecookies.licenseplatequest.domain.model.Player
import com.getmecookies.licenseplatequest.ui.screens.players.PlayerNameError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class ManagePlayersUiState(
    val loading: Boolean = true,
    /** Players currently on the trip, in the order they joined. */
    val onTrip: List<Player> = emptyList(),
    /** Active roster players not yet on the trip, alphabetized. */
    val available: List<Player> = emptyList(),
    val newName: String = "",
    val newNameError: PlayerNameError? = null,
    val addingNew: Boolean = false,
)

/**
 * Manage the roster of a single trip (reached from the Active Trip overflow menu): add players
 * already in the roster, create-and-add brand-new players, and remove players from the trip.
 * Removing only unlinks the player from this trip — it never deletes them from the roster.
 */
class ManagePlayersViewModel(
    savedStateHandle: SavedStateHandle,
    private val tripRepository: TripRepository,
    private val playerRepository: PlayerRepository,
) : ViewModel() {

    private val tripId: UUID = UUID.fromString(
        checkNotNull(savedStateHandle[ARG_TRIP_ID]) { "ManagePlayers requires a '$ARG_TRIP_ID' argument" },
    )

    private val _uiState = MutableStateFlow(ManagePlayersUiState())
    val uiState: StateFlow<ManagePlayersUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                playerRepository.observePlayers(),
                tripRepository.observePlayerIdsForTrip(tripId),
            ) { players, memberIds ->
                val byId = players.associateBy { it.id }
                val onTrip = memberIds.mapNotNull { byId[it] } // join order
                val available = players.filter { it.id !in memberIds.toSet() } // already alphabetical
                onTrip to available
            }.collect { (onTrip, available) ->
                _uiState.update {
                    it.copy(loading = false, onTrip = onTrip, available = available)
                }
            }
        }
    }

    fun onNewNameChange(value: String) {
        _uiState.update { it.copy(newName = value, newNameError = null) }
    }

    /** Create a brand-new player and add them to this trip in one step. */
    fun onAddNew() {
        val current = _uiState.value
        if (current.addingNew) return
        val name = current.newName.trim()
        if (name.isBlank()) {
            _uiState.update { it.copy(newNameError = PlayerNameError.BLANK) }
            return
        }
        _uiState.update { it.copy(addingNew = true, newNameError = null) }
        viewModelScope.launch {
            if (playerRepository.nameExists(name)) {
                _uiState.update {
                    it.copy(addingNew = false, newNameError = PlayerNameError.DUPLICATE)
                }
                return@launch
            }
            val id = playerRepository.addPlayer(name)
            tripRepository.addPlayerToTrip(tripId, id)
            _uiState.update { it.copy(addingNew = false, newName = "", newNameError = null) }
        }
    }

    fun onAddExisting(playerId: UUID) {
        viewModelScope.launch { tripRepository.addPlayerToTrip(tripId, playerId) }
    }

    fun onRemove(playerId: UUID) {
        viewModelScope.launch { tripRepository.removePlayerFromTrip(tripId, playerId) }
    }

    companion object {
        const val ARG_TRIP_ID = "tripId"
    }
}
