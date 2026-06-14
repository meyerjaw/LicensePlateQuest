package com.getmecookies.licenseplatequest.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.getmecookies.licenseplatequest.data.repository.RegionRepository
import com.getmecookies.licenseplatequest.data.repository.SettingsRepository
import com.getmecookies.licenseplatequest.data.seed.SampleDataSeeder
import com.getmecookies.licenseplatequest.domain.Analytics
import com.getmecookies.licenseplatequest.domain.NoOpAnalytics
import com.getmecookies.licenseplatequest.domain.UiPreferences
import com.getmecookies.licenseplatequest.domain.model.HomeLocation
import com.getmecookies.licenseplatequest.domain.model.RegionOption
import com.getmecookies.licenseplatequest.domain.model.ThemeMode
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
import java.util.UUID

/** In-progress home-location edit (the set-home dialog). */
data class HomeDialogState(val city: String, val regionId: UUID?)

/** ViewModel for the Settings screen: theme, haptics, and the home-location editor (#8). */
class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val regionRepository: RegionRepository,
    private val sampleDataSeeder: SampleDataSeeder,
    private val uiPreferences: UiPreferences,
    private val analytics: Analytics = NoOpAnalytics,
) : ViewModel() {

    val themeMode: StateFlow<ThemeMode> = settingsRepository.themeMode
    val hapticsEnabled: StateFlow<Boolean> = settingsRepository.hapticsEnabled
    val soundEnabled: StateFlow<Boolean> = settingsRepository.soundEnabled
    val tripRemindersEnabled: StateFlow<Boolean> = settingsRepository.tripRemindersEnabled
    val analyticsEnabled: StateFlow<Boolean> = settingsRepository.analyticsEnabled
    val home: StateFlow<HomeLocation?> = settingsRepository.home

    val regionOptions: StateFlow<List<RegionOption>> =
        regionRepository.observeRegionOptions()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _homeDialog = MutableStateFlow<HomeDialogState?>(null)
    val homeDialog: StateFlow<HomeDialogState?> = _homeDialog.asStateFlow()

    fun onThemeModeSelected(mode: ThemeMode) {
        settingsRepository.setThemeMode(mode)
        logSettingChanged("theme", mode.name.lowercase())
    }

    fun onHapticsToggled(enabled: Boolean) {
        settingsRepository.setHapticsEnabled(enabled)
        logSettingChanged("haptics", enabled)
    }

    fun onSoundToggled(enabled: Boolean) {
        settingsRepository.setSoundEnabled(enabled)
        logSettingChanged("sound", enabled)
    }

    fun onTripRemindersToggled(enabled: Boolean) {
        settingsRepository.setTripRemindersEnabled(enabled)
        logSettingChanged("trip_reminders", enabled)
    }

    fun onAnalyticsToggled(enabled: Boolean) {
        settingsRepository.setAnalyticsEnabled(enabled)
        // Note: turning analytics off is gated out by ConsentGatedAnalytics (the flag is already
        // false by the time we log), so only opt-ins are recorded — by design.
        logSettingChanged("analytics", enabled)
    }

    private fun logSettingChanged(key: String, value: Any) {
        analytics.event("setting_changed", mapOf("key" to key, "value" to value))
    }

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

    /**
     * Re-run the first-run onboarding wizard. Clears the completion flag and resets the saved step;
     * the app root observes the flag and swaps the wizard back in immediately.
     */
    fun restartOnboarding() {
        uiPreferences.onboardingStep = 0
        uiPreferences.setOnboardingComplete(false)
    }

    /**
     * One-shot human-readable result of the debug-only "Seed sample data" action. Debug-only, so the
     * message is deliberately detailed (region count, created trip id, exception text) to make a
     * failed seed self-diagnosing in the confirmation Toast.
     */
    private val _seedEvents = Channel<String>(Channel.BUFFERED)
    val seedEvents: Flow<String> = _seedEvents.receiveAsFlow()

    /**
     * Debug-only: populate a rich, varied sample dataset (roster + completed/in-progress/active
     * trips with finds) so a fresh install is one tap from exercising every surface. Gated to debug
     * builds at the call site. Delegates to [SampleDataSeeder]; always reports a result via
     * [seedEvents].
     */
    fun seedSampleData() {
        viewModelScope.launch {
            _seedEvents.send(sampleDataSeeder.seed())
        }
    }

    /** Debug-only: erase all trips, players, and progress (keeps bundled regions). */
    fun wipeAllData() {
        viewModelScope.launch {
            _seedEvents.send(sampleDataSeeder.wipeAllData())
        }
    }
}
