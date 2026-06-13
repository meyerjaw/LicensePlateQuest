package com.getmecookies.licenseplatequest.ui.screens.trips

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.getmecookies.licenseplatequest.data.local.AppDatabase
import com.getmecookies.licenseplatequest.data.local.entity.PlateRegionEntity
import com.getmecookies.licenseplatequest.data.repository.PlayerRepository
import com.getmecookies.licenseplatequest.data.repository.RegionRepository
import com.getmecookies.licenseplatequest.data.repository.SettingsRepository
import com.getmecookies.licenseplatequest.data.repository.TripRepository
import com.getmecookies.licenseplatequest.domain.FakeAnalytics
import com.getmecookies.licenseplatequest.notifications.FakeReminderScheduler
import com.getmecookies.licenseplatequest.testutil.MainDispatcherRule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.UUID

/**
 * Verifies the New Trip flow emits the `trip_created` analytics event (with the right params) when
 * a trip is saved. Uses the default unconfined dispatcher rule so the save coroutine runs; the test
 * waits on [NewTripViewModel.saved] (flipped right after the event) before asserting.
 */
@RunWith(RobolectricTestRunner::class)
class NewTripAnalyticsTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var db: AppDatabase
    private val analytics = FakeAnalytics()
    private lateinit var viewModel: NewTripViewModel
    private lateinit var regionId1: UUID
    private lateinit var regionId2: UUID
    private lateinit var playerId: UUID

    @Before
    fun setUp() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val r1 = region("TX", 1)
        val r2 = region("CO", 2)
        db.plateRegionDao().upsertAll(listOf(r1, r2))
        regionId1 = r1.id
        regionId2 = r2.id
        val players = PlayerRepository(db.playerDao(), db.tripPlayerDao(), db.eventLogDao())
        playerId = players.addPlayer("Alice")
        viewModel = NewTripViewModel(
            tripRepository = TripRepository(db, FakeReminderScheduler()),
            regionRepository = RegionRepository(db.plateRegionDao()),
            playerRepository = players,
            settingsRepository = SettingsRepository(context),
            analytics = analytics,
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun onSave_logsTripCreatedEvent_withParams() = runBlocking {
        viewModel.onStopCityChange(0, "Austin")
        viewModel.onStopRegionSelected(0, regionId1)
        viewModel.onStopCityChange(1, "Denver")
        viewModel.onStopRegionSelected(1, regionId2)
        viewModel.onTogglePlayer(playerId)

        viewModel.onSave()
        withTimeout(5_000) { viewModel.saved.first { it } }

        assertEquals(listOf("trip_created"), analytics.eventNames())
        val params = analytics.paramsOf("trip_created")!!
        assertEquals(1, params["player_count"])
        assertEquals(2, params["stop_count"])
        assertEquals(false, params["has_end_date"])
    }

    private fun region(code: String, order: Int) = PlateRegionEntity(
        id = UUID.randomUUID(),
        countryCode = "US",
        regionCode = code,
        name = code,
        bird = "",
        motto = "",
        flower = "",
        funFacts = "[]",
        plateImagePath = "",
        rarityScore = 0.0,
        centerLat = 0.0,
        centerLng = 0.0,
        displayOrder = order,
        additionalInfo = "{}",
    )
}
