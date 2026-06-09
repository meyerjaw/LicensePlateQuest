package com.getmecookies.licenseplatequest.ui.screens.passport

import android.content.Context
import android.os.Looper
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.getmecookies.licenseplatequest.data.local.AppDatabase
import com.getmecookies.licenseplatequest.data.local.entity.GameTypeEntity
import com.getmecookies.licenseplatequest.data.local.entity.PlateRegionEntity
import com.getmecookies.licenseplatequest.data.map.MapRepository
import com.getmecookies.licenseplatequest.data.repository.SpottingRepository
import com.getmecookies.licenseplatequest.data.repository.TripRepository
import com.getmecookies.licenseplatequest.data.seed.RegionSeeder
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

/** The Passport VM aggregates spottings across trips into the lifetime collection. */
@RunWith(RobolectricTestRunner::class)
class PassportViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var db: AppDatabase
    private lateinit var trips: TripRepository
    private lateinit var spotting: SpottingRepository
    private lateinit var mapRepository: MapRepository
    private lateinit var txId: UUID
    private lateinit var coId: UUID

    @Before
    fun setUp() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        trips = TripRepository(db, FakeReminderScheduler())
        spotting = SpottingRepository(db)
        mapRepository = MapRepository(context)

        val tx = region("TX", 1)
        val co = region("CO", 2)
        db.plateRegionDao().upsertAll(listOf(tx, co))
        txId = tx.id
        coId = co.id
        db.gameTypeDao().upsert(
            GameTypeEntity(UUID.randomUUID(), RegionSeeder.LICENSE_PLATE_CODE, "License Plate", ""),
        )
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun collectsFoundStatesAcrossTrips() = runBlocking {
        createActiveTrip()
        spotting.markState("TX")
        createActiveTrip() // demotes the first trip
        spotting.markState("CO")

        val vm = PassportViewModel(mapRepository, spotting)
        awaitUntil { !vm.uiState.value.loading && vm.uiState.value.collectedCount == 2 }

        assertEquals(setOf("TX", "CO"), vm.uiState.value.foundCodes)
        assertEquals(48, vm.uiState.value.remaining)
    }

    private suspend fun createActiveTrip(): UUID = trips.createTrip(
        name = "Trip",
        originCity = "Austin",
        originRegionId = txId,
        destinationCity = "Denver",
        destinationRegionId = coId,
        startDate = LocalDate.now(),
        endDate = null,
        playerIds = emptyList(),
    )

    private fun awaitUntil(timeoutMs: Long = 5000, condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (!condition() && System.currentTimeMillis() < deadline) {
            shadowOf(Looper.getMainLooper()).idle()
            Thread.sleep(5)
        }
        assert(condition()) { "Condition not met within ${timeoutMs}ms" }
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
