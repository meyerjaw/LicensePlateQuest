package com.getmecookies.licenseplatequest.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.getmecookies.licenseplatequest.data.local.AppDatabase
import com.getmecookies.licenseplatequest.data.local.entity.GameTypeEntity
import com.getmecookies.licenseplatequest.data.local.entity.PlateRegionEntity
import com.getmecookies.licenseplatequest.data.seed.RegionSeeder
import com.getmecookies.licenseplatequest.notifications.FakeReminderScheduler
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate
import java.util.UUID

/** Achievement evaluation + earned-once persistence over in-memory Room. */
@RunWith(RobolectricTestRunner::class)
class AchievementRepositoryTest {

    private lateinit var db: AppDatabase
    private lateinit var trips: TripRepository
    private lateinit var spotting: SpottingRepository
    private lateinit var achievements: AchievementRepository
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
        achievements = AchievementRepository(db, RegionRepository(db.plateRegionDao()))

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
    fun earnsAchievements_andOnlyReportsEachOnce() = runBlocking {
        val tripId = createActiveTrip()
        spotting.markState("TX")

        // First find unlocks "first plate".
        assertTrue(achievements.evaluateAndPersist().contains("first_plate"))
        // Re-evaluating reports nothing new.
        assertTrue(achievements.evaluateAndPersist().isEmpty())

        // Completing the trip unlocks "first trip" on the next evaluation.
        trips.endTrip(tripId)
        assertTrue(achievements.evaluateAndPersist().contains("first_trip"))

        assertEquals(setOf("first_plate", "first_trip"), achievements.observeEarned().first())
        assertFalse(achievements.observeEarned().first().contains("collect_10"))
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
