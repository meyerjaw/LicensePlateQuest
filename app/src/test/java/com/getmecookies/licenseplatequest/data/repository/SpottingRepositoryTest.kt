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

/**
 * Tests for marking license-plate spottings against the active trip (SPEC §6), including the
 * idempotent mark, multi-player attribution, and found-count propagation. In-memory Room under
 * Robolectric, seeded with two regions and the single `license_plate` GameType so trips get a
 * game instance.
 */
@RunWith(RobolectricTestRunner::class)
class SpottingRepositoryTest {

    private lateinit var db: AppDatabase
    private lateinit var trips: TripRepository
    private lateinit var players: PlayerRepository
    private lateinit var spotting: SpottingRepository
    private lateinit var txId: UUID
    private lateinit var coId: UUID

    @Before
    fun setUp() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        trips = TripRepository(db, FakeReminderScheduler())
        players = PlayerRepository(db.playerDao(), db.tripPlayerDao(), db.eventLogDao())
        spotting = SpottingRepository(db)

        val tx = region("TX", 1)
        val co = region("CO", 2)
        db.plateRegionDao().upsertAll(listOf(tx, co))
        txId = tx.id
        coId = co.id
        // createTrip only spins up a game instance when this GameType exists.
        db.gameTypeDao().upsert(
            GameTypeEntity(UUID.randomUUID(), RegionSeeder.LICENSE_PLATE_CODE, "License Plate", ""),
        )
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun markState_createsSpotting_andIsIdempotent() = runBlocking {
        createActiveTrip()

        assertTrue(spotting.markState("TX"))
        assertEquals(setOf("TX"), spotting.observeFoundCodesForActiveTrip().first())

        // Marking again does nothing and doesn't duplicate.
        assertFalse(spotting.markState("TX"))
        assertEquals(1, spotting.observeFoundCodesForActiveTrip().first().size)
    }

    @Test
    fun markState_noActiveTrip_returnsFalse() = runBlocking {
        assertFalse(spotting.markState("TX"))
    }

    @Test
    fun markState_recordsAttribution() = runBlocking {
        val alice = players.addPlayer("Alice")
        createActiveTrip(playerIds = listOf(alice))

        spotting.markState("TX", listOf(alice))

        val detail = spotting.getStateDetail("TX")!!
        assertTrue(detail.found)
        assertEquals(setOf(alice), detail.initialAttribution)
    }

    @Test
    fun setAttribution_replacesCredits() = runBlocking {
        val alice = players.addPlayer("Alice")
        val bob = players.addPlayer("Bob")
        createActiveTrip(playerIds = listOf(alice, bob))

        spotting.markState("TX", listOf(alice))
        spotting.setAttribution("TX", listOf(bob))

        assertEquals(setOf(bob), spotting.getStateDetail("TX")!!.initialAttribution)
    }

    @Test
    fun markState_leavesFindPendingCelebration() = runBlocking {
        createActiveTrip()
        spotting.markState("TX")

        // A fresh find is uncelebrated until the map animates it (#20).
        assertEquals(setOf("TX"), spotting.observeUncelebratedCodesForActiveTrip().first())
    }

    @Test
    fun markCelebrated_clearsPending_butKeepsFound() = runBlocking {
        createActiveTrip()
        spotting.markState("TX")

        spotting.markCelebrated(setOf("TX"))

        assertTrue(spotting.observeUncelebratedCodesForActiveTrip().first().isEmpty())
        // Still found — only the animation flag changed.
        assertEquals(setOf("TX"), spotting.observeFoundCodesForActiveTrip().first())
    }

    @Test
    fun unmarkState_removesSpotting() = runBlocking {
        createActiveTrip()
        spotting.markState("TX")

        spotting.unmarkState("TX")

        assertTrue(spotting.observeFoundCodesForActiveTrip().first().isEmpty())
        assertFalse(spotting.getStateDetail("TX")!!.found)
    }

    @Test
    fun foundCount_reflectsMarks_onTripListItem() = runBlocking {
        val tripId = createActiveTrip()
        spotting.markState("TX")
        spotting.markState("CO")

        val item = trips.observeTripListItems().first().first { it.id == tripId }
        assertEquals(2, item.foundCount)
    }

    @Test
    fun getStateDetail_autoCreditsSolePlayer_forUnfoundState() = runBlocking {
        val solo = players.addPlayer("Solo")
        createActiveTrip(playerIds = listOf(solo))

        val detail = spotting.getStateDetail("CO")!!
        assertFalse(detail.found)
        assertEquals(setOf(solo), detail.initialAttribution)
    }

    @Test
    fun getStateDetail_noAutoCredit_withMultiplePlayers() = runBlocking {
        val a = players.addPlayer("A")
        val b = players.addPlayer("B")
        createActiveTrip(playerIds = listOf(a, b))

        assertTrue(spotting.getStateDetail("CO")!!.initialAttribution.isEmpty())
    }

    @Test
    fun observeLifetimeStates_unionsAcrossTrips_anddedupes() = runBlocking {
        // Trip 1: catch TX.
        createActiveTrip()
        spotting.markState("TX")
        // Trip 2 becomes active (demotes trip 1); catch CO, and TX again.
        createActiveTrip()
        spotting.markState("CO")
        spotting.markState("TX")

        val lifetime = spotting.observeLifetimeStates().first()
        // Union across both trips, with TX collapsed to a single lifetime entry.
        assertEquals(
            listOf("CO", "TX"),
            lifetime.map { it.code }) // ordered by name (Colorado, Texas)
        assertEquals(2, lifetime.size)
    }

    private suspend fun createActiveTrip(playerIds: List<UUID> = emptyList()): UUID =
        trips.createTrip(
            name = "Trip",
            originCity = "Austin",
            originRegionId = txId,
            destinationCity = "Denver",
            destinationRegionId = coId,
            startDate = LocalDate.now(),
            endDate = null,
            playerIds = playerIds,
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
