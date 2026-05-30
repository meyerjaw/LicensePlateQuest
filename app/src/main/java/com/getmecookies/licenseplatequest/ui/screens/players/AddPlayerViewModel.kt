package com.getmecookies.licenseplatequest.ui.screens.players

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.getmecookies.licenseplatequest.data.repository.PlayerRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * State for the full-screen Add Player flow. Kept deliberately small for now — future
 * phases will add more fields here (e.g. avatar, color) without touching the list screen.
 */
data class AddPlayerUiState(
    val name: String = "",
    val error: String? = null,
    val saving: Boolean = false,
)

/**
 * ViewModel for [AddPlayerScreen]. Owns name entry, validation, and the save call, then
 * signals completion via a one-shot [saved] flag the screen observes to navigate back.
 */
class AddPlayerViewModel(
    private val playerRepository: PlayerRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddPlayerUiState())
    val uiState: StateFlow<AddPlayerUiState> = _uiState.asStateFlow()

    private val _saved = MutableStateFlow(false)
    val saved: StateFlow<Boolean> = _saved.asStateFlow()

    fun onNameChange(value: String) {
        _uiState.update { it.copy(name = value, error = null) }
    }

    fun onSave() {
        val current = _uiState.value
        if (current.saving) return
        if (current.name.isBlank()) {
            _uiState.update { it.copy(error = "Name can't be empty") }
            return
        }
        _uiState.update { it.copy(saving = true) }
        viewModelScope.launch {
            playerRepository.addPlayer(current.name)
            _saved.value = true
        }
    }
}
