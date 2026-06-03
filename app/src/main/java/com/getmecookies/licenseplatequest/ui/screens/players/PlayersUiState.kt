package com.getmecookies.licenseplatequest.ui.screens.players

import com.getmecookies.licenseplatequest.domain.model.Player
import com.getmecookies.licenseplatequest.domain.model.PlayerListItem

/**
 * UI state for the Players roster screen. Adding a player is now a full screen
 * (AddPlayerScreen); the only dialog here is in-place name editing. Deletion is swipe-to-delete
 * with an in-place undo window handled by the shared `SwipeToDeleteRow`, so it needs no state
 * here — the commit just soft-deletes and the observed roster drops the player.
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
}
