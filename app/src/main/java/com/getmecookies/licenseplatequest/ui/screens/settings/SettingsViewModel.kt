package com.getmecookies.licenseplatequest.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.getmecookies.licenseplatequest.data.repository.RegionRepository
import com.getmecookies.licenseplatequest.data.repository.SettingsRepository
import com.getmecookies.licenseplatequest.domain.model.HomeLocation
import com.getmecookies.licenseplatequest.domain.model.RegionOption
import com.getmecookies.licenseplatequest.domain.model.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.util.UUID

/** In-progress home-location edit (the set-home dialog). */
data class HomeDialogState(val city: String, val regionId: UUID?)

/** ViewModel for the Settings screen: theme, haptics, and the home-location editor (#8). */
class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    regionRepository: RegionRepository,
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
}
