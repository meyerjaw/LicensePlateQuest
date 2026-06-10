package com.getmecookies.licenseplatequest.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.getmecookies.licenseplatequest.data.local.AppDatabase
import com.getmecookies.licenseplatequest.data.local.entity.GameTypeEntity
import com.getmecookies.licenseplatequest.data.local.entity.PlateRegionEntity
import com.getmecookies.licenseplatequest.data.seed.RegionSeeder
import com.getmecookies.licenseplatequest.notifications.FakeReminderScheduler
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate
import java.util.UUID

/** Recap stats: the journey timeline (ordered finds) and the busiest day. */
@RunWith(RobolectricTestRunner::class)
class CelebrationRepositoryTest {

    private lateinit var db: AppDatabase
    private lateinit var trips: TripRepository
    private lateinit var spotting: SpottingRepository
    private lateinit var celebration: CelebrationRepository
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
        celebration = CelebrationRepository(db)

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
    fun stats_includeJourneyTimelineInOrder_andBusiestDay() = runBlocking {
        val tripId = createActiveTrip()
        spotting.markState("TX")
        spotting.markState("CO")

        val stats = celebration.getStats(tripId)!!

        assertEquals(listOf("TX", "CO"), stats.timeline.map { it.code })
        assertNotNull(stats.busiestDayText)
        assertTrue(stats.busiestDayText!!.startsWith("2 states"))
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
