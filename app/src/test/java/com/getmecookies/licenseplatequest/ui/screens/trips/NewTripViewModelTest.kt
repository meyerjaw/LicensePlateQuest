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
import kotlinx.coroutines.test.StandardTestDispatcher
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

    // StandardTestDispatcher keeps the ViewModel's init-time flow collects dormant (we never
    // advance it), so these synchronous handler tests run deterministically without async races.
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(StandardTestDispatcher())

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

    @Test
    fun startsWithTwoStops_andAddStopAppends() {
        assertEquals(2, viewModel.uiState.value.stops.size)
        viewModel.onAddStop()
        assertEquals(3, viewModel.uiState.value.stops.size)
    }

    @Test
    fun removeStop_respectsTwoStopMinimum() {
        viewModel.onRemoveStop(0)
        assertEquals(2, viewModel.uiState.value.stops.size)

        viewModel.onAddStop()
        viewModel.onRemoveStop(1)
        assertEquals(2, viewModel.uiState.value.stops.size)
    }

    @Test
    fun stopEdits_updateCityAndRegion() {
        val region = java.util.UUID.randomUUID()
        viewModel.onStopCityChange(0, "Austin")
        viewModel.onStopRegionSelected(0, region)

        val stop = viewModel.uiState.value.stops[0]
        assertEquals("Austin", stop.city)
        assertEquals(region, stop.regionId)
    }

    @Test
    fun moveStopDown_reordersTheList() {
        viewModel.onStopCityChange(0, "A")
        viewModel.onStopCityChange(1, "B")
        viewModel.onMoveStopDown(0)
        assertEquals(listOf("B", "A"), viewModel.uiState.value.stops.map { it.city })
    }
}
