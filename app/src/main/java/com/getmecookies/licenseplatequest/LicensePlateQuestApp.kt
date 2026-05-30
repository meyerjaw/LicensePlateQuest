package com.getmecookies.licenseplatequest

import android.app.Application
import com.getmecookies.licenseplatequest.di.AppContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Application entry point. Builds the [AppContainer] and kicks off bundled-data seeding
 * off the main thread on startup (SPEC §11 — bundled state data loading).
 */
class LicensePlateQuestApp : Application() {

    lateinit var container: AppContainer
        private set

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        applicationScope.launch {
            container.regionSeeder.seedIfNeeded()
        }
    }
}
