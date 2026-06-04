package com.getmecookies.licenseplatequest

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.getmecookies.licenseplatequest.domain.model.ThemeMode
import com.getmecookies.licenseplatequest.ui.navigation.AppRoot
import com.getmecookies.licenseplatequest.ui.theme.LicensePlateQuestTheme

/**
 * Single-activity host (SPEC §9 — Jetpack Compose). All screens are composables behind the
 * navigation graph in [AppRoot]. The theme follows the user's Settings choice (live).
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val settings = (application as LicensePlateQuestApp).container.settingsRepository
        setContent {
            val themeMode by settings.themeMode.collectAsStateWithLifecycle()
            val darkTheme = when (themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            LicensePlateQuestTheme(darkTheme = darkTheme) {
                AppRoot()
            }
        }
    }
}
