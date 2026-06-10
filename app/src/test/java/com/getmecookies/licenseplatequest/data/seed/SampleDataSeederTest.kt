package com.getmecookies.licenseplatequest.data.seed

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.getmecookies.licenseplatequest.data.local.AppDatabase
import com.getmecookies.licenseplatequest.data.repository.AchievementRepository
import com.getmecookies.licenseplatequest.data.repository.PlayerRepository
import com.getmecookies.licenseplatequest.data.repository.RegionRepository
import com.getmecookies.licenseplatequest.data.repository.SpottingRepository
import com.getmecookies.licenseplatequest.data.repository.TripRepository
import com.getmecookies.licenseplatequest.domain.model.TripStatus
import com.getmecookies.licenseplatequest.notifications.FakeReminderScheduler
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Instant

/**
 * Verifies the debug sample seeder builds the intended variety: a full roster, trips in every
 * lifecycle state (including an overdue one and a 50/50 sweep), and back-dated history.
 */
@RunWith(RobolectricTestRunner::class)
class SampleDataSeederTest {

    private lateinit var db: AppDatabase
    private lateinit var trips: TripRepository
    private lateinit var players: PlayerRepository
    private lateinit var spotting: SpottingRepository
    private lateinit var seeder: SampleDataSeeder

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val regions = RegionRepository(db.plateRegionDao())
        players = PlayerRepository(db.playerDao(), db.tripPlayerDao(), db.eventLogDao())
        trips = TripRepository(db, FakeReminderScheduler())
        spotting = SpottingRepository(db)
        val regionSeeder = RegionSeeder(context, db.plateRegionDao(), db.gameTypeDao())
        seeder = SampleDataSeeder(
            database = db,
            regionRepository = regions,
            playerRepository = players,
            tripRepository = trips,
            spottingRepository = spotting,
            achievementRepository = AchievementRepository(db, regions),
            regionSeeder = regionSeeder,
        )
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun seed_buildsVariedRosterTripsAndHistory() = runBlocking {
        val msg = seeder.seed()
        assertTrue("unexpected result: $msg", msg.startsWith("Sample data added"))

        assertEquals(6, players.observePlayers().first().size)

        val list = trips.observeTripListItems().first()
        assertEquals(6, list.size)
        assertEquals(1, list.count { it.status == TripStatus.ACTIVE })
        assertEquals(3, list.count { it.status == TripStatus.COMPLETED })
        assertEquals(2, list.count { it.status == TripStatus.IN_PROGRESS })
        // One in-progress trip is past its end date.
        assertTrue("expected an overdue trip", list.any { it.isOverdue })
        // The cross-country sweep found all 50 — lights up the completed-map styling.
        assertTrue("expected a 50/50 trip", list.any { it.isComplete })
        // Completed trips have a real (back-dated) duration, not "0 minutes".
        assertTrue(
            "expected multi-day completed durations",
            list.filter { it.status == TripStatus.COMPLETED }
                .all { it.durationLabel?.contains("day") == true },
        )

        // The lifetime collection spans the full map, with first-spotted dates in the past.
        val lifetime = spotting.observeLifetimeStates().first()
        assertTrue("expected a full lifetime collection, got ${lifetime.size}", lifetime.size >= 50)
        assertTrue(lifetime.all { it.firstFoundAt.isBefore(Instant.now()) })
    }

    @Test
    fun wipe_clearsUserDataButKeepsBundledRegions() = runBlocking {
        seeder.seed()
        assertTrue(trips.observeTripListItems().first().isNotEmpty())

        val msg = seeder.wipeAllData()
        assertTrue("unexpected result: $msg", msg.startsWith("All trips"))

        assertTrue(trips.observeTripListItems().first().isEmpty())
        assertTrue(players.observePlayers().first().isEmpty())
        assertTrue(spotting.observeLifetimeStates().first().isEmpty())
        // Bundled reference data survives, so the app still works after a wipe.
        assertTrue(RegionRepository(db.plateRegionDao()).getAllRegions().isNotEmpty())
    }
}
