package com.getmecookies.licenseplatequest.ui.screens.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.getmecookies.licenseplatequest.data.repository.PlayerRepository
import com.getmecookies.licenseplatequest.data.repository.RegionRepository
import com.getmecookies.licenseplatequest.data.repository.SettingsRepository
import com.getmecookies.licenseplatequest.data.repository.TripRepository
import com.getmecookies.licenseplatequest.domain.UiPreferences
import com.getmecookies.licenseplatequest.domain.model.RegionOption
import com.getmecookies.licenseplatequest.domain.model.TripStop
import com.getmecookies.licenseplatequest.ui.PlayerColors
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID

/** A player added during onboarding — kept so the first trip can include them automatically. */
data class AddedPlayer(val id: UUID, val name: String, val colorToken: String?)

/**
 * State for the first-run onboarding wizard. All data is written through the real repositories as
 * the user advances (players on add, home on the home step, the trip on the trip step), so bailing
 * out at any point never loses work.
 */
data class OnboardingUiState(
    val step: Int = 0,
    val regionOptions: List<RegionOption> = emptyList(),
    // Home step.
    val homeRegionId: UUID? = null,
    val homeCity: String = "",
    // Players step.
    val players: List<AddedPlayer> = emptyList(),
    val playerNameDraft: String = "",
    val playerColorDraft: String? = null,
    val savingPlayer: Boolean = false,
    // Trip step.
    val tripName: String = "",
    val tripNameEdited: Boolean = false,
    val originRegionId: UUID? = null,
    val originCity: String = "",
    val destRegionId: UUID? = null,
    val destCity: String = "",
    val startDate: LocalDate = LocalDate.now(),
    val endDate: LocalDate? = null,
    val savingTrip: Boolean = false,
) {
    val homeValid: Boolean get() = homeRegionId != null && homeCity.isNotBlank()
    val playerDraftValid: Boolean get() = playerNameDraft.isNotBlank()
    val tripValid: Boolean
        get() = tripName.isNotBlank() && originRegionId != null && originCity.isNotBlank() &&
                destRegionId != null && destCity.isNotBlank()

    /** Resolve a region id to its 2-letter code (for the auto-prefilled name). */
    fun regionCode(regionId: UUID?): String? = regionOptions.firstOrNull { it.id == regionId }?.code
}

/**
 * Drives the onboarding wizard (first-run trip setup). Steps: Welcome, Set home, Add players,
 * Create first trip, Ready. The current step is persisted so a force-quit resumes where the user
 * left off; finishing or skipping flips [UiPreferences.onboardingComplete], which the app root
 * observes to swap to the main app.
 */
class OnboardingViewModel(
    private val uiPreferences: UiPreferences,
    private val settingsRepository: SettingsRepository,
    private val playerRepository: PlayerRepository,
    private val tripRepository: TripRepository,
    regionRepository: RegionRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        OnboardingUiState(step = uiPreferences.onboardingStep.coerceIn(0, LAST_STEP)),
    )
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    init {
        // Region options for the home + trip selectors.
        viewModelScope.launch {
            regionRepository.observeRegionOptions().collect { options ->
                _uiState.update { it.copy(regionOptions = options) }
            }
        }
        // Prefill home + the trip origin from any previously-saved home location.
        prefillHome()
        // If onboarding is restarted from Settings while this (activity-scoped) VM is still alive,
        // reset to a clean first step instead of resuming wherever it last was. drop(1) ignores the
        // current value so the initial prefill above isn't clobbered.
        viewModelScope.launch {
            uiPreferences.onboardingComplete.drop(1).collect { complete ->
                if (!complete) {
                    uiPreferences.onboardingStep = 0
                    _uiState.update { OnboardingUiState(regionOptions = it.regionOptions) }
                    prefillHome()
                }
            }
        }
    }

    private fun prefillHome() {
        settingsRepository.home.value?.let { home ->
            _uiState.update {
                it.copy(
                    homeRegionId = home.regionId,
                    homeCity = home.city,
                    originRegionId = home.regionId,
                    originCity = home.city,
                ).withPrefilledTripName()
            }
        }
    }

    // ---- Navigation ------------------------------------------------------------------------

    private fun setStep(step: Int) {
        val clamped = step.coerceIn(0, LAST_STEP)
        uiPreferences.onboardingStep = clamped
        _uiState.update { it.copy(step = clamped) }
    }

    fun back() = setStep(_uiState.value.step - 1)

    /** Advance without a side effect (Welcome's Get started, the players step's Continue/Skip). */
    fun next() = setStep(_uiState.value.step + 1)

    /** Finish or skip the whole wizard — flips the completion flag so the app root swaps in. */
    fun finish() {
        uiPreferences.onboardingStep = 0
        uiPreferences.setOnboardingComplete(true)
    }

    // ---- Home step -------------------------------------------------------------------------

    fun onHomeRegion(regionId: UUID) = _uiState.update { it.copy(homeRegionId = regionId) }
    fun onHomeCity(city: String) = _uiState.update { it.copy(homeCity = city) }

    /** Save the chosen home, prefill it into the trip origin, and advance. */
    fun saveHomeAndNext() {
        val state = _uiState.value
        val regionId = state.homeRegionId
        if (regionId != null && state.homeCity.isNotBlank()) {
            settingsRepository.setHome(regionId, state.homeCity.trim())
            _uiState.update {
                it.copy(
                    originRegionId = it.originRegionId ?: regionId,
                    originCity = it.originCity.ifBlank { state.homeCity.trim() },
                )
            }
        }
        next()
    }

    // ---- Players step ----------------------------------------------------------------------

    fun onPlayerNameDraft(name: String) = _uiState.update { it.copy(playerNameDraft = name) }
    fun onPlayerColorDraft(token: String) = _uiState.update { it.copy(playerColorDraft = token) }

    fun addPlayer() {
        val state = _uiState.value
        val name = state.playerNameDraft.trim()
        if (name.isEmpty() || state.savingPlayer) return
        // Fall back to the first unused swatch so every player gets a distinct color by default.
        val color = state.playerColorDraft
            ?: PlayerColors.firstUnusedToken(state.players.map { it.colorToken })
        _uiState.update { it.copy(savingPlayer = true) }
        viewModelScope.launch {
            val id = playerRepository.addPlayer(name, color)
            _uiState.update {
                it.copy(
                    players = it.players + AddedPlayer(id, name, color),
                    playerNameDraft = "",
                    playerColorDraft = null,
                    savingPlayer = false,
                )
            }
        }
    }

    // ---- Trip step -------------------------------------------------------------------------

    fun onTripName(name: String) =
        _uiState.update { it.copy(tripName = name, tripNameEdited = true) }

    fun onOriginRegion(regionId: UUID) =
        _uiState.update { it.copy(originRegionId = regionId).withPrefilledTripName() }

    fun onOriginCity(city: String) =
        _uiState.update { it.copy(originCity = city).withPrefilledTripName() }

    fun onDestRegion(regionId: UUID) =
        _uiState.update { it.copy(destRegionId = regionId).withPrefilledTripName() }

    fun onDestCity(city: String) =
        _uiState.update { it.copy(destCity = city).withPrefilledTripName() }

    fun onStartDate(date: LocalDate) = _uiState.update {
        it.copy(
            startDate = date,
            endDate = it.endDate?.takeIf { end -> !end.isBefore(date) },
        ).withPrefilledTripName()
    }

    fun onEndDate(date: LocalDate?) = _uiState.update { it.copy(endDate = date) }

    /**
     * Recompute the auto-prefilled trip name unless the user has taken over the field — same rule as
     * the New Trip form: "Origin to Destination, CODE - Month Year", with the destination carrying
     * its state code. A single known endpoint stays open as "Origin to ".
     */
    private fun OnboardingUiState.withPrefilledTripName(): OnboardingUiState {
        if (tripNameEdited) return this
        val originLabel = originCity.trim().ifBlank { regionCode(originRegionId).orEmpty() }
        val destCode = regionCode(destRegionId)
        val destLabel = when {
            destCity.isBlank() && destCode == null -> ""
            destCity.isBlank() -> destCode.orEmpty()
            destCode != null -> "${destCity.trim()}, $destCode"
            else -> destCity.trim()
        }
        val labels = listOf(originLabel, destLabel).filter { it.isNotBlank() }
        val monthYear = startDate.format(MONTH_YEAR)
        val prefilled = when {
            labels.size >= 2 -> labels.joinToString(" to ") + " - $monthYear"
            labels.size == 1 -> "${labels.first()} to "
            else -> ""
        }
        return copy(tripName = prefilled)
    }

    /** Create the first trip (with the onboarding players) and advance to the Ready step. */
    fun createTripAndNext() {
        val state = _uiState.value
        if (!state.tripValid || state.savingTrip) return
        _uiState.update { it.copy(savingTrip = true) }
        viewModelScope.launch {
            tripRepository.createTrip(
                name = state.tripName.trim(),
                stops = listOf(
                    TripStop(regionId = state.originRegionId!!, city = state.originCity.trim()),
                    TripStop(regionId = state.destRegionId!!, city = state.destCity.trim()),
                ),
                startDate = state.startDate,
                endDate = state.endDate,
                playerIds = state.players.map { it.id },
            )
            _uiState.update { it.copy(savingTrip = false) }
            next()
        }
    }

    companion object {
        /** Welcome, Set home, Add players, Create trip, Ready. */
        const val TOTAL_STEPS = 5
        const val LAST_STEP = TOTAL_STEPS - 1

        private val MONTH_YEAR: DateTimeFormatter =
            DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())
    }
}
