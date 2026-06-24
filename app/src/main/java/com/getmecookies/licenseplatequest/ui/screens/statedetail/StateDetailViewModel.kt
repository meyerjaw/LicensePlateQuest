package com.getmecookies.licenseplatequest.ui.screens.statedetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.getmecookies.licenseplatequest.data.repository.SpottingRepository
import com.getmecookies.licenseplatequest.domain.Analytics
import com.getmecookies.licenseplatequest.domain.NoOpAnalytics
import com.getmecookies.licenseplatequest.domain.model.StateDetailData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

/** Which confirmation dialog (if any) is showing on State Detail. */
enum class StateDetailDialog { NONE, CONFIRM_UNMARK, CONFIRM_DISCARD }

data class StateDetailUiState(
    val loading: Boolean = true,
    val data: StateDetailData? = null,
    val dialog: StateDetailDialog = StateDetailDialog.NONE,
    /** Players currently selected for attribution (playtest note #17). */
    val selectedPlayerIds: Set<UUID> = emptySet(),
    /** The persisted attribution baseline, to detect unsaved edits on a found state. */
    val savedAttribution: Set<UUID> = emptySet(),
    /** Set true once a brand-new mark is committed, so the screen can fire a celebration. */
    val justMarked: Boolean = false,
    /** Set true after marking, signaling the screen to navigate back to the map. */
    val markComplete: Boolean = false,
) {
    /** A found state whose attribution differs from what's saved — shows the save (✓) action. */
    val hasUnsavedAttribution: Boolean
        get() = data?.found == true && selectedPlayerIds != savedAttribution
}

/**
 * ViewModel for State Detail (SPEC section 6). Loads the state's bundled facts plus its
 * found-status on the active trip. Marking commits immediately (the explicit "Mark as found"
 * tap is confirmation enough) and signals the screen to return to the map; unmarking is
 * gated behind a confirmation dialog. The state code comes from the navigation argument.
 */
class StateDetailViewModel(
    savedStateHandle: SavedStateHandle,
    private val spottingRepository: SpottingRepository,
    private val analytics: Analytics = NoOpAnalytics,
) : ViewModel() {

    private val regionCode: String = checkNotNull(savedStateHandle[ARG_CODE]) {
        "StateDetail requires a '$ARG_CODE' argument"
    }

    private val _uiState = MutableStateFlow(StateDetailUiState())
    val uiState: StateFlow<StateDetailUiState> = _uiState.asStateFlow()

    init {
        reload()
    }

    private fun reload() {
        viewModelScope.launch {
            val data = spottingRepository.getStateDetail(regionCode)
            val attribution = data?.initialAttribution ?: emptySet()
            _uiState.update {
                it.copy(
                    loading = false,
                    data = data,
                    selectedPlayerIds = attribution,
                    savedAttribution = attribution,
                )
            }
        }
    }

    /**
     * Toggle a player's attribution. This updates only the local selection; for an unfound state
     * it's committed on mark, and for a found state it's committed via [onSaveAttribution] (the
     * top-bar ✓), so edits are deliberate (playtest note #17).
     */
    fun onTogglePlayer(playerId: UUID) {
        val next = _uiState.value.selectedPlayerIds.toMutableSet().apply {
            if (!add(playerId)) remove(playerId)
        }
        _uiState.update { it.copy(selectedPlayerIds = next) }
    }

    /** Save edited attribution for an already-found state, then close the screen. */
    fun onSaveAttribution() {
        val state = _uiState.value
        if (state.data?.found != true) return
        val toSave = state.selectedPlayerIds
        viewModelScope.launch {
            spottingRepository.setAttribution(regionCode, toSave.toList())
            analytics.event("attribution_set", mapOf("player_count" to toSave.size))
            _uiState.update { it.copy(savedAttribution = toSave, markComplete = true) }
        }
    }

    /** Mark immediately (no confirmation), then signal the screen to return to the map. */
    fun onMarkClick() {
        viewModelScope.launch {
            val players = _uiState.value.selectedPlayerIds
            val created = spottingRepository.markState(regionCode, players.toList())
            if (created) {
                analytics.event(
                    "state_marked",
                    mapOf(
                        "region" to regionCode,
                        "attributed_player_count" to players.size,
                        "source" to "detail",
                    ),
                )
            }
            _uiState.update { it.copy(justMarked = created, markComplete = true) }
        }
    }

    fun onUnmarkClick() {
        _uiState.update { it.copy(dialog = StateDetailDialog.CONFIRM_UNMARK) }
    }

    /** Show the "discard unsaved attribution?" warning (when leaving with unsaved edits). */
    fun onConfirmDiscardChanges() {
        _uiState.update { it.copy(dialog = StateDetailDialog.CONFIRM_DISCARD) }
    }

    fun onDismissDialog() {
        _uiState.update { it.copy(dialog = StateDetailDialog.NONE) }
    }

    fun onConfirmUnmark() {
        viewModelScope.launch {
            spottingRepository.unmarkState(regionCode)
            analytics.event("state_unmarked", mapOf("region" to regionCode))
            _uiState.update { it.copy(dialog = StateDetailDialog.NONE) }
            reload()
        }
    }

    /** Consume the one-shot "just marked" flag after the screen reacts to it. */
    fun onCelebrationShown() {
        _uiState.update { it.copy(justMarked = false) }
    }

    companion object {
        const val ARG_CODE = "code"
    }
}
