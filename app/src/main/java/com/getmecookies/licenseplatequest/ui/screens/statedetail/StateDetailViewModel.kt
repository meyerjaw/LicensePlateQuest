package com.getmecookies.licenseplatequest.ui.screens.statedetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.getmecookies.licenseplatequest.data.repository.SpottingRepository
import com.getmecookies.licenseplatequest.domain.model.StateDetailData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Which confirmation dialog (if any) is showing on State Detail. */
enum class StateDetailDialog { NONE, CONFIRM_UNMARK }

data class StateDetailUiState(
    val loading: Boolean = true,
    val data: StateDetailData? = null,
    val dialog: StateDetailDialog = StateDetailDialog.NONE,
    /** Set true once a brand-new mark is committed, so the screen can fire a celebration. */
    val justMarked: Boolean = false,
    /** Set true after marking, signaling the screen to navigate back to the map. */
    val markComplete: Boolean = false,
)

/**
 * ViewModel for State Detail (SPEC section 6). Loads the state's bundled facts plus its
 * found-status on the active trip. Marking commits immediately (the explicit "Mark as found"
 * tap is confirmation enough) and signals the screen to return to the map; unmarking is
 * gated behind a confirmation dialog. The state code comes from the navigation argument.
 */
class StateDetailViewModel(
    savedStateHandle: SavedStateHandle,
    private val spottingRepository: SpottingRepository,
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
            _uiState.update { it.copy(loading = false, data = data) }
        }
    }

    /** Mark immediately (no confirmation), then signal the screen to return to the map. */
    fun onMarkClick() {
        viewModelScope.launch {
            val created = spottingRepository.markState(regionCode)
            _uiState.update { it.copy(justMarked = created, markComplete = true) }
        }
    }

    fun onUnmarkClick() {
        _uiState.update { it.copy(dialog = StateDetailDialog.CONFIRM_UNMARK) }
    }

    fun onDismissDialog() {
        _uiState.update { it.copy(dialog = StateDetailDialog.NONE) }
    }

    fun onConfirmUnmark() {
        viewModelScope.launch {
            spottingRepository.unmarkState(regionCode)
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
