package com.getmecookies.licenseplatequest.ui.screens.passport

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.getmecookies.licenseplatequest.data.map.MapRepository
import com.getmecookies.licenseplatequest.data.repository.AchievementRepository
import com.getmecookies.licenseplatequest.data.repository.RegionRepository
import com.getmecookies.licenseplatequest.data.repository.SpottingRepository
import com.getmecookies.licenseplatequest.data.repository.TripRepository
import com.getmecookies.licenseplatequest.domain.AchievementStats
import com.getmecookies.licenseplatequest.domain.model.LifetimeState
import com.getmecookies.licenseplatequest.ui.map.UsMapShapes
import java.time.Instant
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * The lifetime "Plate Passport" (cross-trip collection): a filled lifetime map, an all-time
 * collected counter, and the list of states caught across every trip with first-spotted dates.
 */
data class PassportUiState(
    val loading: Boolean = true,
    val shapes: UsMapShapes? = null,
    val collected: List<LifetimeState> = emptyList(),
    /** Codes first caught on the *current* active trip — highlighted as new to the collection. */
    val newToCollection: Set<String> = emptySet(),
    /** Rare-plate state codes, for the "Rare" badge (rare-plate moments). */
    val rareCodes: Set<String> = emptySet(),
    /** Earned achievement ids, for the achievements section. */
    val earnedAchievements: Set<String> = emptySet(),
    /** Earned achievement id → when it was unlocked, for the detail sheet. */
    val achievementEarnedAt: Map<String, Instant> = emptyMap(),
    /** Live progress snapshot, so locked badges can show how close the player is. */
    val achievementStats: AchievementStats = AchievementStats(),
) {
    val collectedCount: Int get() = collected.size
    val remaining: Int get() = (PassportViewModel.TOTAL_STATES - collectedCount).coerceAtLeast(0)
    val foundCodes: Set<String> get() = collected.mapTo(HashSet()) { it.code }
}

class PassportViewModel(
    mapRepository: MapRepository,
    spottingRepository: SpottingRepository,
    tripRepository: TripRepository,
    regionRepository: RegionRepository,
    achievementRepository: AchievementRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PassportUiState())
    val uiState: StateFlow<PassportUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val shapes = mapRepository.loadShapes()
            _uiState.update { it.copy(shapes = shapes) }
        }
        viewModelScope.launch {
            _uiState.update { it.copy(rareCodes = regionRepository.getRareCodes()) }
        }
        viewModelScope.launch {
            achievementRepository.observeEarned().collect { earned ->
                _uiState.update { it.copy(earnedAchievements = earned) }
            }
        }
        viewModelScope.launch {
            achievementRepository.observeEarnedDates().collect { dates ->
                _uiState.update { it.copy(achievementEarnedAt = dates) }
            }
        }
        viewModelScope.launch {
            // A state is "new to the collection" when its first-ever catch was on the active trip.
            // This drives the core content (map + collected list), so it flips loading off as soon
            // as the states arrive — it does NOT wait on the achievement-progress snapshot below.
            combine(
                spottingRepository.observeLifetimeStates(),
                tripRepository.observeActiveTrip(),
            ) { states, activeTrip ->
                val newCodes = activeTrip?.id?.let { active ->
                    states.filterTo(mutableSetOf()) { it.firstTripId == active }
                        .mapTo(HashSet()) { it.code }
                } ?: emptySet()
                states to newCodes
            }.collect { (states, newCodes) ->
                _uiState.update {
                    it.copy(loading = false, collected = states, newToCollection = newCodes)
                }
            }
        }
        viewModelScope.launch {
            // Achievement-progress snapshot, computed off the critical render path. Recomputed when
            // the collection changes (locked-badge bars stay current) but never blocks first paint —
            // the bars simply fill in a beat after the map and list appear.
            spottingRepository.observeLifetimeStates().collect {
                val stats = achievementRepository.getStats()
                _uiState.update { it.copy(achievementStats = stats) }
            }
        }
    }

    companion object {
        const val TOTAL_STATES = 50
    }
}
