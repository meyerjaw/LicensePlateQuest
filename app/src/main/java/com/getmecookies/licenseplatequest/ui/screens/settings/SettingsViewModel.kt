package com.getmecookies.licenseplatequest.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.getmecookies.licenseplatequest.data.repository.PlayerRepository
import com.getmecookies.licenseplatequest.data.repository.RegionRepository
import com.getmecookies.licenseplatequest.data.repository.SettingsRepository
import com.getmecookies.licenseplatequest.data.repository.SpottingRepository
import com.getmecookies.licenseplatequest.data.repository.TripRepository
import com.getmecookies.licenseplatequest.domain.model.HomeLocation
import com.getmecookies.licenseplatequest.domain.model.RegionOption
import com.getmecookies.licenseplatequest.domain.model.ThemeMode
import com.getmecookies.licenseplatequest.domain.model.TripStop
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.UUID

/** In-progress home-location edit (the set-home dialog). */
data class HomeDialogState(val city: String, val regionId: UUID?)

/** ViewModel for the Settings screen: theme, haptics, and the home-location editor (#8). */
class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val regionRepository: RegionRepository,
    private val playerRepository: PlayerRepository,
    private val tripRepository: TripRepository,
    private val spottingRepository: SpottingRepository,
) : ViewModel() {

    val themeMode: StateFlow<ThemeMode> = settingsRepository.themeMode
    val hapticsEnabled: StateFlow<Boolean> = settingsRepository.hapticsEnabled
    val tripRemindersEnabled: StateFlow<Boolean> = settingsRepository.tripRemindersEnabled
    val home: StateFlow<HomeLocation?> = settingsRepository.home

    val regionOptions: StateFlow<List<RegionOption>> =
        regionRepository.observeRegionOptions()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _homeDialog = MutableStateFlow<HomeDialogState?>(null)
    val homeDialog: StateFlow<HomeDialogState?> = _homeDialog.asStateFlow()

    fun onThemeModeSelected(mode: ThemeMode) = settingsRepository.setThemeMode(mode)

    fun onHapticsToggled(enabled: Boolean) = settingsRepository.setHapticsEnabled(enabled)

    fun onTripRemindersToggled(enabled: Boolean) =
        settingsRepository.setTripRemindersEnabled(enabled)

    fun onEditHome() {
        val current = home.value
        _homeDialog.value = HomeDialogState(city = current?.city ?: "", regionId = current?.regionId)
    }

    fun onHomeCityChange(city: String) = _homeDialog.update { it?.copy(city = city) }

    fun onHomeRegionSelected(id: UUID) = _homeDialog.update { it?.copy(regionId = id) }

    fun onHomeDialogDismiss() {
        _homeDialog.value = null
    }

    fun onHomeDialogSave() {
        val dialog = _homeDialog.value ?: return
        val regionId = dialog.regionId ?: return
        val city = dialog.city.trim()
        if (city.isEmpty()) return
        settingsRepository.setHome(regionId, city)
        _homeDialog.value = null
    }

    fun onClearHome() = settingsRepository.clearHome()

    /** One-shot signal that sample data finished seeding (debug-only "Seed sample data"). */
    private val _seedEvents = Channel<Unit>(Channel.BUFFERED)
    val seedEvents: Flow<Unit> = _seedEvents.receiveAsFlow()

    /**
     * Debug-only: populate a few players and a multi-stop trip with some finds so a fresh install
     * is one tap from a useful test state. Gated to debug builds at the call site. No-ops if the
     * bundled regions haven't seeded yet.
     */
    fun seedSampleData() {
        viewModelScope.launch {
            val regionsByCode = regionRepository.getAllRegions().associateBy { it.regionCode }
            fun stop(code: String, city: String): TripStop? =
                regionsByCode[code]?.let { TripStop(it.id, city) }
            val stops = listOfNotNull(
                stop("OH", "Columbus"),
                stop("KY", "Louisville"),
                stop("TN", "Nashville"),
                stop("FL", "Orlando"),
            )
            if (stops.size < 2) return@launch

            val alex = playerRepository.addPlayer("Alex")
            val sam = playerRepository.addPlayer("Sam")
            val jordan = playerRepository.addPlayer("Jordan")
            tripRepository.createTrip(
                name = "Sample Road Trip",
                stops = stops,
                startDate = LocalDate.now().minusDays(2),
                endDate = LocalDate.now().plusDays(5),
                playerIds = listOf(alex, sam, jordan),
            )
            // A few finds so the map fills, the leaderboard ranks, and stats have content.
            spottingRepository.markState("OH", listOf(alex))
            spottingRepository.markState("KY", listOf(alex, sam))
            spottingRepository.markState("TN", listOf(jordan))
            _seedEvents.send(Unit)
        }
    }
}
