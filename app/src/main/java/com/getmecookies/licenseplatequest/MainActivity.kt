package com.getmecookies.licenseplatequest

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.getmecookies.licenseplatequest.ui.navigation.AppRoot
import com.getmecookies.licenseplatequest.ui.theme.LicensePlateQuestTheme

/**
 * Single-activity host (SPEC §9 — Jetpack Compose). All screens are composables behind the
 * navigation graph in [AppRoot].
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LicensePlateQuestTheme {
                AppRoot()
            }
        }
    }
}
