package com.getmecookies.licenseplatequest.ui.screens.trips

import android.content.Context
import android.os.Looper
import androidx.lifecycle.SavedStateHandle
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.getmecookies.licenseplatequest.data.local.AppDatabase
import com.getmecookies.licenseplatequest.data.local.entity.PlateRegionEntity
import com.getmecookies.licenseplatequest.data.repository.PlayerRepository
import com.getmecookies.licenseplatequest.data.repository.RegionRepository
import com.getmecookies.licenseplatequest.data.repository.TripRepository
import com.getmecookies.licenseplatequest.notifications.FakeReminderScheduler
import com.getmecookies.licenseplatequest.testutil.MainDispatcherRule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
 * Tests for the Manage trip (edit) ViewModel. The ViewModel loads the trip asynchronously in
 * init, so each test awaits load completion before asserting. Uses the default background Room
 * executor (so createTrip's withTransaction works) and polls via the Robolectric main looper.
 */
@RunWith(RobolectricTestRunner::class)
class ManageTripViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var db: AppDatabase
    private lateinit var trips: TripRepository
    private lateinit var players: PlayerRepository
    private lateinit var originId: UUID
    private lateinit var destId: UUID

    @Before
    fun setUp() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        trips = TripRepository(db, FakeReminderScheduler())
        players = PlayerRepository(db.playerDao(), db.tripPlayerDao(), db.eventLogDao())
        val origin = region("TX", 1)
        val dest = region("CO", 2)
        db.plateRegionDao().upsertAll(listOf(origin, dest))
        originId = origin.id
        destId = dest.id
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun loadsTrip_andStartsNotDirty() = runBlocking {
        val tripId = createTrip(name = "Original", playerIds = listOf(players.addPlayer("Alice")))
        val vm = loadedViewModel(tripId)

        assertEquals("Original", vm.uiState.value.name)
        assertFalse(vm.isDirty())
    }

    @Test
    fun editingName_marksDirty() = runBlocking {
        val tripId = createTrip(name = "Original", playerIds = listOf(players.addPlayer("Alice")))
        val vm = loadedViewModel(tripId)

        vm.onNameChange("Changed")

        assertTrue(vm.isDirty())
    }

    @Test
    fun togglingPlayer_marksDirty() = runBlocking {
        val p1 = players.addPlayer("Alice")
        val p2 = players.addPlayer("Bob")
        val tripId = createTrip(playerIds = listOf(p1))
        val vm = loadedViewModel(tripId)

        vm.onTogglePlayer(p2)

        assertTrue(vm.isDirty())
    }

    @Test
    fun save_appliesFieldEdits_andPlayerDiff() = runBlocking {
        val p1 = players.addPlayer("Alice")
        val p2 = players.addPlayer("Bob")
        val tripId = createTrip(name = "Original", playerIds = listOf(p1))
        val vm = loadedViewModel(tripId)

        vm.onNameChange("Edited")
        vm.onEndDateChange(LocalDate.now().plusDays(5))
        vm.onTogglePlayer(p2) // add Bob
        vm.onTogglePlayer(p1) // remove Alice
        vm.onSave()
        awaitUntil { vm.saved.value }

        val trip = trips.getTrip(tripId)!!
        assertEquals("Edited", trip.name)
        assertEquals(setOf(p2), trips.observePlayerIdsForTrip(tripId).first().toSet())
    }

    @Test
    fun endDateChange_clampsToStart() = runBlocking {
        val tripId = createTrip(playerIds = listOf(players.addPlayer("Alice")))
        val vm = loadedViewModel(tripId)

        vm.onStartDateChange(LocalDate.of(2026, 6, 15))
        vm.onEndDateChange(LocalDate.of(2026, 6, 10))

        assertEquals(LocalDate.of(2026, 6, 15), vm.uiState.value.endDate)
    }

    private fun loadedViewModel(tripId: UUID): ManageTripViewModel {
        val vm = ManageTripViewModel(
            savedStateHandle = SavedStateHandle(
                mapOf(ManageTripViewModel.ARG_TRIP_ID to tripId.toString()),
            ),
            tripRepository = trips,
            regionRepository = RegionRepository(db.plateRegionDao()),
            playerRepository = players,
        )
        awaitUntil { !vm.uiState.value.loading }
        return vm
    }

    private suspend fun createTrip(
        name: String = "Trip",
        endDate: LocalDate? = null,
        playerIds: List<UUID>,
    ): UUID = trips.createTrip(
        name = name,
        originCity = "Austin",
        originRegionId = originId,
        destinationCity = "Denver",
        destinationRegionId = destId,
        startDate = LocalDate.now(),
        endDate = endDate,
        playerIds = playerIds,
    )

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
