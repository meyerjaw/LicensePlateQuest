package com.getmecookies.licenseplatequest.ui.screens.settings

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.getmecookies.licenseplatequest.data.backup.BackupRepository
import com.getmecookies.licenseplatequest.data.local.AppDatabase
import com.getmecookies.licenseplatequest.data.repository.AchievementRepository
import com.getmecookies.licenseplatequest.data.repository.PlayerRepository
import com.getmecookies.licenseplatequest.data.repository.RegionRepository
import com.getmecookies.licenseplatequest.data.repository.SettingsRepository
import com.getmecookies.licenseplatequest.data.repository.SpottingRepository
import com.getmecookies.licenseplatequest.data.repository.TripRepository
import com.getmecookies.licenseplatequest.data.seed.RegionSeeder
import com.getmecookies.licenseplatequest.data.seed.SampleDataSeeder
import com.getmecookies.licenseplatequest.domain.FakeAnalytics
import com.getmecookies.licenseplatequest.domain.UiPreferences
import com.getmecookies.licenseplatequest.domain.model.ThemeMode
import com.getmecookies.licenseplatequest.notifications.FakeReminderScheduler
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Verifies the Settings toggles fire `setting_changed` analytics events (key + new value), using a
 * [FakeAnalytics] double. Construction uses an in-memory Room DB; only the toggle paths are exercised.
 */
@RunWith(RobolectricTestRunner::class)
class SettingsViewModelTest {

    private lateinit var db: AppDatabase
    private lateinit var analytics: FakeAnalytics
    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val regionRepository = RegionRepository(db.plateRegionDao())
        val sampleDataSeeder = SampleDataSeeder(
            database = db,
            regionRepository = regionRepository,
            playerRepository = PlayerRepository(db.playerDao(), db.tripPlayerDao(), db.eventLogDao()),
            tripRepository = TripRepository(db, FakeReminderScheduler()),
            spottingRepository = SpottingRepository(db),
            achievementRepository = AchievementRepository(db, regionRepository),
            regionSeeder = RegionSeeder(context, db.plateRegionDao(), db.gameTypeDao()),
        )
        analytics = FakeAnalytics()
        val settingsRepository = SettingsRepository(context)
        viewModel = SettingsViewModel(
            settingsRepository = settingsRepository,
            regionRepository = regionRepository,
            sampleDataSeeder = sampleDataSeeder,
            uiPreferences = UiPreferences(context),
            backupRepository = BackupRepository(db, settingsRepository),
            analytics = analytics,
        )
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun onSoundToggled_logsSettingChanged() {
        viewModel.onSoundToggled(false)

        val params = analytics.paramsOf("setting_changed")
        assertTrue("setting_changed should fire", params != null)
        assertEquals("sound", params!!["key"])
        assertEquals(false, params["value"])
    }

    @Test
    fun onThemeModeSelected_logsThemeKeyWithLowercasedValue() {
        viewModel.onThemeModeSelected(ThemeMode.DARK)

        val params = analytics.paramsOf("setting_changed")
        assertEquals("theme", params?.get("key"))
        assertEquals("dark", params?.get("value"))
    }

    @Test
    fun onAnalyticsToggled_logsAnalyticsOptIn() {
        viewModel.onAnalyticsToggled(true)

        val params = analytics.paramsOf("setting_changed")
        assertEquals("analytics", params?.get("key"))
        assertEquals(true, params?.get("value"))
    }
}
