package com.getmecookies.licenseplatequest.ui.screens.players

import com.getmecookies.licenseplatequest.domain.model.Player
import com.getmecookies.licenseplatequest.domain.model.PlayerListItem

/**
 * UI state for the Players roster screen. Adding a player is now a full screen
 * (AddPlayerScreen); the dialogs here cover only in-place edit and delete-confirm.
 */
data class PlayersUiState(
    val players: List<PlayerListItem> = emptyList(),
    val loading: Boolean = true,
    val dialog: PlayerDialog = PlayerDialog.None,
)

/** Mutually-exclusive dialog states for the Players roster screen. */
sealed interface PlayerDialog {
    data object None : PlayerDialog

    /** Edit-name dialog for an existing player. */
    data class Edit(
        val player: Player,
        val name: String,
        val error: PlayerNameError? = null,
    ) : PlayerDialog

    /** Delete confirmation. [tripCount] > 0 triggers the "on existing trips" warning. */
    data class ConfirmDelete(
        val player: Player,
        val tripCount: Int,
    ) : PlayerDialog
}
