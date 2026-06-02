package com.getmecookies.licenseplatequest.ui.screens.players

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.getmecookies.licenseplatequest.data.repository.PlayerRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * State for the full-screen Add Player flow. Kept deliberately small for now — future
 * phases will add more fields here (e.g. avatar, color) without touching the list screen.
 */
/**
 * A typed player-name validation failure. The screen resolves this to a localized string
 * (supplying the entered name for [DUPLICATE]) so no user-facing text lives in the ViewModel.
 */
enum class PlayerNameError { BLANK, DUPLICATE }

data class AddPlayerUiState(
    val name: String = "",
    val error: PlayerNameError? = null,
    val saving: Boolean = false,
)

/**
 * ViewModel for [AddPlayerScreen]. Owns name entry, validation, and the save call, then
 * signals completion via [savedPlayerId] (the new player's id). The screen observes this to
 * navigate back and, when launched from the New Trip flow, to report which player was added.
 */
class AddPlayerViewModel(
    private val playerRepository: PlayerRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddPlayerUiState())
    val uiState: StateFlow<AddPlayerUiState> = _uiState.asStateFlow()

    private val _savedPlayerId = MutableStateFlow<UUID?>(null)
    val savedPlayerId: StateFlow<UUID?> = _savedPlayerId.asStateFlow()

    fun onNameChange(value: String) {
        _uiState.update { it.copy(name = value, error = null) }
    }

    fun onSave() {
        val current = _uiState.value
        if (current.saving) return
        val name = current.name.trim()
        if (name.isBlank()) {
            _uiState.update { it.copy(error = PlayerNameError.BLANK) }
            return
        }
        _uiState.update { it.copy(saving = true, error = null) }
        viewModelScope.launch {
            if (playerRepository.nameExists(name)) {
                _uiState.update { it.copy(saving = false, error = PlayerNameError.DUPLICATE) }
                return@launch
            }
            val id = playerRepository.addPlayer(name)
            _savedPlayerId.value = id
        }
    }
}
