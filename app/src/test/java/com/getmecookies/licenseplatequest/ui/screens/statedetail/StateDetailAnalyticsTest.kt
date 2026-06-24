package com.getmecookies.licenseplatequest.ui.screens.statedetail

import android.content.Context
import android.os.Looper
import androidx.lifecycle.SavedStateHandle
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.getmecookies.licenseplatequest.data.local.AppDatabase
import com.getmecookies.licenseplatequest.data.local.entity.GameTypeEntity
import com.getmecookies.licenseplatequest.data.local.entity.PlateRegionEntity
import com.getmecookies.licenseplatequest.data.repository.PlayerRepository
import com.getmecookies.licenseplatequest.data.repository.SpottingRepository
import com.getmecookies.licenseplatequest.data.repository.TripRepository
import com.getmecookies.licenseplatequest.data.seed.RegionSeeder
import com.getmecookies.licenseplatequest.domain.FakeAnalytics
import com.getmecookies.licenseplatequest.notifications.FakeReminderScheduler
import com.getmecookies.licenseplatequest.testutil.MainDispatcherRule
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import java.time.LocalDate
import java.util.UUID

/**
 * Verifies State Detail fires the `state_marked` / `state_unmarked` analytics events (PII-free —
 * region code only). Driven through the real reactive pipeline (in-memory Room) with a
 * [FakeAnalytics] double, awaiting the async state updates.
 */
@RunWith(RobolectricTestRunner::class)
class StateDetailAnalyticsTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var db: AppDatabase
    private lateinit var trips: TripRepository
    private lateinit var spotting: SpottingRepository
    private lateinit var regions: List<PlateRegionEntity>

    @Before
    fun setUp() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        trips = TripRepository(db, FakeReminderScheduler())
        spotting = SpottingRepository(db)

        regions = (0 until 3).map { i -> region(code = "S%02d".format(i), order = i) }
        db.plateRegionDao().upsertAll(regions)
        db.gameTypeDao().upsert(
            GameTypeEntity(UUID.randomUUID(), RegionSeeder.LICENSE_PLATE_CODE, "License Plate", ""),
        )
        trips.createTrip(
            name = "Trip",
            originCity = "Austin",
            originRegionId = regions[0].id,
            destinationCity = "Denver",
            destinationRegionId = regions[1].id,
            startDate = LocalDate.now(),
            endDate = null,
            playerIds = emptyList(),
        )
        Unit // keep @Before's return type Unit (createTrip returns a UUID)
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun onMarkClick_logsStateMarked() = runBlocking {
        val analytics = FakeAnalytics()
        val vm = viewModelFor(regions[0].regionCode, analytics)

        vm.onMarkClick()
        awaitUntil { vm.uiState.value.markComplete }

        val params = analytics.paramsOf("state_marked")
        assertTrue("state_marked should fire", params != null)
        assertEquals(regions[0].regionCode, params!!["region"])
        assertEquals(0, params["attributed_player_count"])
        assertEquals("detail", params["source"])
    }

    @Test
    fun onConfirmUnmark_logsStateUnmarked() = runBlocking {
        spotting.markState(regions[0].regionCode)
        val analytics = FakeAnalytics()
        val vm = viewModelFor(regions[0].regionCode, analytics)
        awaitUntil { vm.uiState.value.data?.found == true }

        vm.onConfirmUnmark()
        awaitUntil { analytics.eventNames().contains("state_unmarked") }

        assertEquals(regions[0].regionCode, analytics.paramsOf("state_unmarked")?.get("region"))
    }

    @Test
    fun onSaveAttribution_logsAttributionSet() = runBlocking {
        val playerId = PlayerRepository(db.playerDao(), db.tripPlayerDao(), db.eventLogDao())
            .addPlayer("Sam")
        spotting.markState(regions[0].regionCode) // found, with no attribution yet
        val analytics = FakeAnalytics()
        val vm = viewModelFor(regions[0].regionCode, analytics)
        awaitUntil { vm.uiState.value.data?.found == true }

        vm.onTogglePlayer(playerId) // attribute one player
        vm.onSaveAttribution()
        awaitUntil { analytics.eventNames().contains("attribution_set") }

        assertEquals(1, analytics.paramsOf("attribution_set")?.get("player_count"))
    }

    private fun viewModelFor(code: String, analytics: FakeAnalytics): StateDetailViewModel {
        val vm = StateDetailViewModel(
            savedStateHandle = SavedStateHandle(mapOf(StateDetailViewModel.ARG_CODE to code)),
            spottingRepository = spotting,
            analytics = analytics,
        )
        awaitUntil { !vm.uiState.value.loading }
        return vm
    }

    private fun awaitUntil(timeoutMs: Long = 5000, condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (!condition() && System.currentTimeMillis() < deadline) {
            shadowOf(Looper.getMainLooper()).idle()
            Thread.sleep(5)
        }
        assertTrue("Condition not met within ${timeoutMs}ms", condition())
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
