package com.getmecookies.licenseplatequest.ui.screens.players

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.getmecookies.licenseplatequest.data.repository.PlayerRepository
import com.getmecookies.licenseplatequest.domain.model.Player
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for the Players roster (SPEC section 6). Exposes a single PlayersUiState as a
 * StateFlow and owns the in-place edit/delete flow. Adding a player now lives in
 * AddPlayerScreen, so there's no add-dialog state here. Name validation lives here so the
 * screen stays a thin renderer.
 */
class PlayersViewModel(
    private val playerRepository: PlayerRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlayersUiState())
    val uiState: StateFlow<PlayersUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            playerRepository.observePlayersWithStats().collect { players ->
                _uiState.update { it.copy(players = players, loading = false) }
            }
        }
    }

    // --- Dialog open/close -------------------------------------------------

    fun onEditClick(player: Player) {
        _uiState.update { it.copy(dialog = PlayerDialog.Edit(player = player, name = player.name)) }
    }

    fun onDeleteClick(player: Player) {
        viewModelScope.launch {
            val count = playerRepository.tripCountForPlayer(player.id)
            _uiState.update {
                it.copy(dialog = PlayerDialog.ConfirmDelete(player = player, tripCount = count))
            }
        }
    }

    fun onDismissDialog() {
        _uiState.update { it.copy(dialog = PlayerDialog.None) }
    }

    // --- Text editing ------------------------------------------------------

    fun onDialogNameChange(value: String) {
        _uiState.update { state ->
            when (val dialog = state.dialog) {
                is PlayerDialog.Edit -> state.copy(dialog = dialog.copy(name = value, error = null))
                else -> state
            }
        }
    }

    // --- Confirm actions ---------------------------------------------------

    fun onConfirmEdit() {
        val dialog = _uiState.value.dialog as? PlayerDialog.Edit ?: return
        val error = validate(dialog.name)
        if (error != null) {
            _uiState.update { it.copy(dialog = dialog.copy(error = error)) }
            return
        }
        val trimmed = dialog.name.trim()
        viewModelScope.launch {
            // Reject a clash with another active player (renaming to its own name is fine).
            if (playerRepository.nameExists(trimmed, excludeId = dialog.player.id)) {
                _uiState.update {
                    val d = it.dialog as? PlayerDialog.Edit ?: return@update it
                    it.copy(dialog = d.copy(error = PlayerNameError.DUPLICATE))
                }
                return@launch
            }
            playerRepository.renamePlayer(dialog.player.id, trimmed)
            _uiState.update { it.copy(dialog = PlayerDialog.None) }
        }
    }

    fun onConfirmDelete() {
        val dialog = _uiState.value.dialog as? PlayerDialog.ConfirmDelete ?: return
        viewModelScope.launch {
            playerRepository.deletePlayer(dialog.player.id)
            _uiState.update { it.copy(dialog = PlayerDialog.None) }
        }
    }

    private fun validate(name: String): PlayerNameError? =
        if (name.isBlank()) PlayerNameError.BLANK else null
}
