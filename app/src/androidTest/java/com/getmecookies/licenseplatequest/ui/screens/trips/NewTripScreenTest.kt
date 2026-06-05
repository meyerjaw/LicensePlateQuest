package com.getmecookies.licenseplatequest.ui.screens.trips

import android.content.Context
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.getmecookies.licenseplatequest.data.local.AppDatabase
import com.getmecookies.licenseplatequest.data.repository.PlayerRepository
import com.getmecookies.licenseplatequest.data.repository.RegionRepository
import com.getmecookies.licenseplatequest.data.repository.SettingsRepository
import com.getmecookies.licenseplatequest.data.repository.TripRepository
import com.getmecookies.licenseplatequest.notifications.NoopReminderScheduler
import com.getmecookies.licenseplatequest.ui.theme.LicensePlateQuestTheme
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented test for the New Trip screen driven by a real in-memory ViewModel. Establishes the
 * screen-with-real-ViewModel pattern for Compose UI tests.
 */
@RunWith(AndroidJUnit4::class)
class NewTripScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var db: AppDatabase

    @After
    fun tearDown() {
        if (::db.isInitialized) db.close()
    }

    @Test
    fun savingEmptyForm_surfacesValidationError() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val viewModel = NewTripViewModel(
            tripRepository = TripRepository(db, NoopReminderScheduler),
            regionRepository = RegionRepository(db.plateRegionDao()),
            playerRepository = PlayerRepository(db.playerDao(), db.tripPlayerDao(), db.eventLogDao()),
            settingsRepository = SettingsRepository(context),
        )

        composeTestRule.setContent {
            LicensePlateQuestTheme {
                NewTripScreen(onDone = {}, onAddPlayer = {}, viewModel = viewModel)
            }
        }

        composeTestRule.onNodeWithText("Start trip").performScrollTo().performClick()
        composeTestRule.onNodeWithText("Pick at least one player").assertExists()
    }
}
