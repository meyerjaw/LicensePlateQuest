package com.getmecookies.licenseplatequest.ui.screens.trips

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.getmecookies.licenseplatequest.data.local.AppDatabase
import com.getmecookies.licenseplatequest.data.repository.PlayerRepository
import com.getmecookies.licenseplatequest.data.repository.RegionRepository
import com.getmecookies.licenseplatequest.data.repository.SettingsRepository
import com.getmecookies.licenseplatequest.data.repository.TripRepository
import com.getmecookies.licenseplatequest.notifications.FakeReminderScheduler
import com.getmecookies.licenseplatequest.testutil.MainDispatcherRule
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate

/**
 * Establishes the ViewModel-test pattern: real in-memory repositories under Robolectric, a
 * MainDispatcherRule for viewModelScope. Covers the date-clamping rules (end >= start).
 */
@RunWith(RobolectricTestRunner::class)
class NewTripViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var db: AppDatabase
    private lateinit var viewModel: NewTripViewModel

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        viewModel = NewTripViewModel(
            tripRepository = TripRepository(db, FakeReminderScheduler()),
            regionRepository = RegionRepository(db.plateRegionDao()),
            playerRepository = PlayerRepository(db.playerDao(), db.tripPlayerDao(), db.eventLogDao()),
            settingsRepository = SettingsRepository(context),
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun startDateChange_pushesEndForwardWhenItWouldPrecedeStart() {
        viewModel.onStartDateChange(LocalDate.of(2026, 6, 5))
        viewModel.onEndDateChange(LocalDate.of(2026, 6, 10))
        assertEquals(LocalDate.of(2026, 6, 10), viewModel.uiState.value.endDate)

        viewModel.onStartDateChange(LocalDate.of(2026, 6, 20))
        assertEquals(LocalDate.of(2026, 6, 20), viewModel.uiState.value.startDate)
        assertEquals(LocalDate.of(2026, 6, 20), viewModel.uiState.value.endDate)
    }

    @Test
    fun endDateChange_clampsToStartWhenEarlier() {
        viewModel.onStartDateChange(LocalDate.of(2026, 6, 15))
        viewModel.onEndDateChange(LocalDate.of(2026, 6, 10))
        assertEquals(LocalDate.of(2026, 6, 15), viewModel.uiState.value.endDate)
    }

    @Test
    fun endDateChange_keepsLaterDate() {
        viewModel.onStartDateChange(LocalDate.of(2026, 6, 15))
        viewModel.onEndDateChange(LocalDate.of(2026, 6, 22))
        assertEquals(LocalDate.of(2026, 6, 22), viewModel.uiState.value.endDate)
    }

    @Test
    fun clearEndDate_removesIt() {
        viewModel.onStartDateChange(LocalDate.of(2026, 6, 15))
        viewModel.onEndDateChange(LocalDate.of(2026, 6, 22))
        viewModel.onClearEndDate()
        assertEquals(null, viewModel.uiState.value.endDate)
    }
}
