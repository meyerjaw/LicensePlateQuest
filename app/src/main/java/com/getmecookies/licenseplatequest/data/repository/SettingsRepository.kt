package com.getmecookies.licenseplatequest.data.repository

import android.content.Context
import com.getmecookies.licenseplatequest.domain.model.HomeLocation
import com.getmecookies.licenseplatequest.domain.model.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

/**
 * Reactive store for user settings, backed by SharedPreferences (survives process death). Values
 * are exposed as [StateFlow]s so the UI updates immediately when a setting changes — e.g. the
 * whole app re-themes the moment [setThemeMode] is called.
 */
class SettingsRepository(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private val _themeMode = MutableStateFlow(loadThemeMode())
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _hapticsEnabled = MutableStateFlow(prefs.getBoolean(KEY_HAPTICS, true))
    val hapticsEnabled: StateFlow<Boolean> = _hapticsEnabled.asStateFlow()

    private val _home = MutableStateFlow(loadHome())
    val home: StateFlow<HomeLocation?> = _home.asStateFlow()

    fun setThemeMode(mode: ThemeMode) {
        prefs.edit().putString(KEY_THEME, mode.name).apply()
        _themeMode.value = mode
    }

    fun setHapticsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_HAPTICS, enabled).apply()
        _hapticsEnabled.value = enabled
    }

    fun setHome(regionId: UUID, city: String) {
        prefs.edit()
            .putString(KEY_HOME_REGION, regionId.toString())
            .putString(KEY_HOME_CITY, city)
            .apply()
        _home.value = HomeLocation(regionId, city)
    }

    fun clearHome() {
        prefs.edit().remove(KEY_HOME_REGION).remove(KEY_HOME_CITY).apply()
        _home.value = null
    }

    private fun loadHome(): HomeLocation? {
        val regionId = prefs.getString(KEY_HOME_REGION, null)
            ?.let { runCatching { UUID.fromString(it) }.getOrNull() }
            ?: return null
        val city = prefs.getString(KEY_HOME_CITY, null)?.takeIf { it.isNotBlank() } ?: return null
        return HomeLocation(regionId, city)
    }

    private fun loadThemeMode(): ThemeMode =
        prefs.getString(KEY_THEME, null)
            ?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
            ?: ThemeMode.SYSTEM

    private companion object {
        const val PREFS = "settings_prefs"
        const val KEY_THEME = "theme_mode"
        const val KEY_HAPTICS = "haptics_enabled"
        const val KEY_HOME_REGION = "home_region_id"
        const val KEY_HOME_CITY = "home_city"
    }
}
