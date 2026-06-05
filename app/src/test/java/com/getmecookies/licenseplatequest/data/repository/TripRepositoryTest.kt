package com.getmecookies.licenseplatequest.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.getmecookies.licenseplatequest.data.local.AppDatabase
import com.getmecookies.licenseplatequest.data.local.entity.PlateRegionEntity
import com.getmecookies.licenseplatequest.domain.model.TripStatus
import com.getmecookies.licenseplatequest.notifications.FakeReminderScheduler
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate
import java.util.UUID

/**
 * Repository tests for the core trip invariants (SPEC §7), exercised against an in-memory Room
 * database under Robolectric. Reminders are verified through a [FakeReminderScheduler].
 */
@RunWith(RobolectricTestRunner::class)
class TripRepositoryTest {

    private lateinit var db: AppDatabase
    private lateinit var repo: TripRepository
    private lateinit var players: PlayerRepository
    private val scheduler = FakeReminderScheduler()
    private lateinit var originId: UUID
    private lateinit var destId: UUID

    @Before
    fun setUp() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repo = TripRepository(db, scheduler)
        players = PlayerRepository(db.playerDao(), db.tripPlayerDao(), db.eventLogDao())
        val origin = region("TX", 1)
        val dest = region("CO", 2)
        db.plateRegionDao().upsertAll(listOf(origin, dest))
        originId = origin.id
        destId = dest.id
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun createTrip_makesItActive_andDemotesPreviousActive() = runBlocking {
        val a = newTrip("Trip A")
        val b = newTrip("Trip B")

        assertEquals(TripStatus.IN_PROGRESS, repo.getTrip(a)!!.status)
        assertEquals(TripStatus.ACTIVE, repo.getTrip(b)!!.status)
        assertEquals(b, repo.observeActiveTrip().first()!!.id)
    }

    @Test
    fun createTrip_schedulesReminderOnlyWhenEndDatePresent() = runBlocking {
        val withEnd = newTrip("With end", endDate = LocalDate.now().plusDays(7))
        assertTrue(scheduler.scheduled.any { it.first == withEnd })

        val withoutEnd = newTrip("No end", endDate = null)
        assertTrue(scheduler.scheduled.none { it.first == withoutEnd })
    }

    @Test
    fun endTrip_completesAndCancels_andIsIdempotent() = runBlocking {
        val id = newTrip("Ending")
        repo.endTrip(id)

        val ended = repo.getTrip(id)!!
        assertEquals(TripStatus.COMPLETED, ended.status)
        assertTrue(ended.endedAt != null)
        assertTrue(scheduler.cancelled.contains(id))

        // Second end is a no-op: status and end time unchanged, no duplicate cancel.
        repo.endTrip(id)
        assertEquals(ended.endedAt, repo.getTrip(id)!!.endedAt)
        assertEquals(1, scheduler.cancelled.count { it == id })
    }

    @Test
    fun deleteTrip_removesRow_andCancelsReminder() = runBlocking {
        val id = newTrip("Doomed", endDate = LocalDate.now().plusDays(3))
        repo.deleteTrip(id)

        assertNull(repo.getTrip(id))
        assertTrue(scheduler.cancelled.contains(id))
    }

    @Test
    fun updateTrip_reschedulesReminderOnlyWhenEndDateChanges() = runBlocking {
        val d1 = LocalDate.now().plusDays(5)
        val id = newTrip("Editable", endDate = d1)
        val scheduledAfterCreate = scheduler.scheduled.count { it.first == id }

        // Same end date, only the name changes -> no reschedule, no cancel.
        repo.updateTrip(id, "Renamed", "Austin", originId, "Denver", destId, LocalDate.now(), d1)
        assertEquals(scheduledAfterCreate, scheduler.scheduled.count { it.first == id })
        assertTrue(scheduler.cancelled.none { it == id })

        // Move the end date -> reschedule for the new date.
        val d2 = LocalDate.now().plusDays(9)
        repo.updateTrip(id, "Renamed", "Austin", originId, "Denver", destId, LocalDate.now(), d2)
        assertTrue(scheduler.scheduled.any { it.first == id && it.second == d2 })

        // Clear the end date -> cancel.
        repo.updateTrip(id, "Renamed", "Austin", originId, "Denver", destId, LocalDate.now(), null)
        assertTrue(scheduler.cancelled.contains(id))
    }

    @Test
    fun setActiveTrip_demotesOthers_andDoesNotResurrectCompleted() = runBlocking {
        val a = newTrip("A")
        val b = newTrip("B") // a -> IN_PROGRESS, b -> ACTIVE

        repo.setActiveTrip(a)
        assertEquals(TripStatus.ACTIVE, repo.getTrip(a)!!.status)
        assertEquals(TripStatus.IN_PROGRESS, repo.getTrip(b)!!.status)

        repo.endTrip(a) // a -> COMPLETED
        repo.setActiveTrip(a) // must not resurrect a completed trip
        assertEquals(TripStatus.COMPLETED, repo.getTrip(a)!!.status)
    }

    @Test
    fun addAndRemovePlayer_updatesTripRoster_withoutDuplicates() = runBlocking {
        val p1 = players.addPlayer("Alice")
        val p2 = players.addPlayer("Bob")
        val trip = newTrip("Roster", playerIds = listOf(p1))

        repo.addPlayerToTrip(trip, p2)
        assertEquals(setOf(p1, p2), repo.observePlayerIdsForTrip(trip).first().toSet())

        // Adding an existing member is a no-op.
        repo.addPlayerToTrip(trip, p2)
        assertEquals(2, repo.observePlayerIdsForTrip(trip).first().size)

        repo.removePlayerFromTrip(trip, p1)
        assertEquals(listOf(p2), repo.observePlayerIdsForTrip(trip).first())
    }

    private suspend fun newTrip(
        name: String = "Trip",
        endDate: LocalDate? = null,
        playerIds: List<UUID> = emptyList(),
    ): UUID = repo.createTrip(
        name = name,
        originCity = "Austin",
        originRegionId = originId,
        destinationCity = "Denver",
        destinationRegionId = destId,
        startDate = LocalDate.now(),
        endDate = endDate,
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
