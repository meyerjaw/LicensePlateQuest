package com.getmecookies.licenseplatequest.ui.screens.activetrip

import android.content.Context
import android.os.Looper
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.getmecookies.licenseplatequest.data.local.AppDatabase
import com.getmecookies.licenseplatequest.data.local.entity.GameTypeEntity
import com.getmecookies.licenseplatequest.data.local.entity.PlateRegionEntity
import com.getmecookies.licenseplatequest.data.map.MapRepository
import com.getmecookies.licenseplatequest.data.repository.SettingsRepository
import com.getmecookies.licenseplatequest.data.repository.AchievementRepository
import com.getmecookies.licenseplatequest.data.repository.RegionRepository
import com.getmecookies.licenseplatequest.data.repository.SpottingRepository
import com.getmecookies.licenseplatequest.data.repository.TripRepository
import com.getmecookies.licenseplatequest.data.seed.RegionSeeder
import com.getmecookies.licenseplatequest.domain.AlbersUsaProjection
import com.getmecookies.licenseplatequest.domain.CelebrationTracker
import com.getmecookies.licenseplatequest.domain.CityLocator
import com.getmecookies.licenseplatequest.domain.FakeAnalytics
import com.getmecookies.licenseplatequest.domain.GeoPoint
import com.getmecookies.licenseplatequest.domain.UiPreferences
import com.getmecookies.licenseplatequest.domain.model.TripStatus
import com.getmecookies.licenseplatequest.notifications.FakeReminderScheduler
import com.getmecookies.licenseplatequest.testutil.MainDispatcherRule
import com.getmecookies.licenseplatequest.ui.screens.celebration.CelebrationMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
 * Tests the celebration logic on the Active Trip ViewModel: the once-per-trip 50/50 (SPEC §6/§8)
 * and the manual-end flow. Driven through the real reactive pipeline (in-memory Room), awaiting
 * the async state updates.
 */
@RunWith(RobolectricTestRunner::class)
class ActiveTripViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var db: AppDatabase
    private lateinit var trips: TripRepository
    private lateinit var spotting: SpottingRepository
    private lateinit var celebrationTracker: CelebrationTracker
    private lateinit var mapRepository: MapRepository
    private lateinit var uiPreferences: UiPreferences
    private lateinit var settings: SettingsRepository
    private lateinit var regionRepository: RegionRepository
    private lateinit var achievements: AchievementRepository
    private val cityLocator = FakeCityLocator()
    private lateinit var regions: List<PlateRegionEntity>

    @Before
    fun setUp() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        trips = TripRepository(db, FakeReminderScheduler())
        spotting = SpottingRepository(db)
        celebrationTracker = CelebrationTracker(context)
        mapRepository = MapRepository(context)
        uiPreferences = UiPreferences(context)
        settings = SettingsRepository(context)
        regionRepository = RegionRepository(db.plateRegionDao())
        achievements = AchievementRepository(db, regionRepository)

        regions = (0 until 50).map { i -> region(code = "S%02d".format(i), order = i) }
        db.plateRegionDao().upsertAll(regions)
        db.gameTypeDao().upsert(
            GameTypeEntity(UUID.randomUUID(), RegionSeeder.LICENSE_PLATE_CODE, "License Plate", ""),
        )
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun markingOneState_doesNotFireFiftyCelebration() = runBlocking {
        createActiveTrip()
        val vm = loadedViewModel()

        spotting.markState(regions[0].regionCode)
        awaitUntil { vm.uiState.value.foundCount == 1 }

        assertNull(vm.uiState.value.celebration)
    }

    @Test
    fun markingAllStates_firesFiftyCelebrationOnce_andSetsTracker() = runBlocking {
        val tripId = createActiveTrip()
        val vm = loadedViewModel()

        regions.forEach { spotting.markState(it.regionCode) }
        awaitUntil { vm.uiState.value.foundCount == 50 }

        val celebration = vm.uiState.value.celebration
        assertTrue(celebration != null && celebration.mode == CelebrationMode.FIFTY_FIFTY)
        assertTrue(celebrationTracker.hasCelebratedFifty(tripId))
    }

    @Test
    fun confirmEndTrip_firesManualEnd_andCompletesTrip() = runBlocking {
        val tripId = createActiveTrip()
        val vm = loadedViewModel()

        vm.onConfirmEndTrip()
        // Ending the trip clears it from ACTIVE, so the VM's active-trip id goes null.
        awaitUntil { vm.uiState.value.tripId == null }

        assertEquals(TripStatus.COMPLETED, trips.getTrip(tripId)!!.status)
        val celebration = vm.uiState.value.celebration
        assertTrue(celebration != null && celebration.mode == CelebrationMode.MANUAL_END)
    }

    @Test
    fun routeStops_reflectTheTripsStops() = runBlocking {
        createActiveTrip()
        val vm = loadedViewModel()

        awaitUntil { vm.uiState.value.routeStops.isNotEmpty() }
        assertEquals(listOf("S00", "S01"), vm.uiState.value.routeStops)
    }

    @Test
    fun foundState_appearsInPendingCelebrations_thenClearsWhenAnimated() = runBlocking {
        createActiveTrip()
        val vm = loadedViewModel()

        spotting.markState(regions[0].regionCode) // "S00"
        // The found set and the pending-celebration set are independent reactive streams; wait for
        // both to register the find before animating it.
        awaitUntil {
            vm.uiState.value.pendingCelebrations.contains("S00") &&
                    vm.uiState.value.foundCodes.contains("S00")
        }

        // The map reports it animated; the find clears from pending but stays found (#20).
        vm.onCelebrationsAnimated(setOf("S00"))
        awaitUntil { vm.uiState.value.pendingCelebrations.isEmpty() }
        assertEquals(setOf("S00"), vm.uiState.value.foundCodes)
    }

    @Test
    fun list_defaultsToAllStates_andSectionTogglesFilter() = runBlocking {
        createActiveTrip()
        val vm = loadedViewModel()

        spotting.markState(regions[0].regionCode)
        awaitUntil { vm.uiState.value.foundCount == 1 }

        // Both sections on by default: every state is listed.
        assertEquals(50, vm.uiState.value.states.size)

        // Hide unfound: only the marked one remains.
        vm.onToggleShowUnfound(false)
        awaitUntil { !vm.uiState.value.showUnfound && vm.uiState.value.states.size == 1 }
        assertEquals(regions[0].regionCode, vm.uiState.value.states.single().code)

        // Hide found too: nothing left.
        vm.onToggleShowFound(false)
        awaitUntil { !vm.uiState.value.showFound && vm.uiState.value.states.isEmpty() }
    }

    @Test
    fun search_reportsMatchesHiddenByASectionToggle() = runBlocking {
        createActiveTrip()
        val vm = loadedViewModel()

        // Hide unfound, then search for an (unfound) state's name.
        vm.onToggleShowUnfound(false)
        awaitUntil { !vm.uiState.value.showUnfound }
        vm.onSearchChange(regions[5].name) // test regions use name == code, e.g. "S05"

        awaitUntil { vm.uiState.value.hiddenUnfoundMatches > 0 }
        assertEquals(0, vm.uiState.value.states.size)
        assertEquals(0, vm.uiState.value.hiddenFoundMatches)
    }

    @Test
    fun mapHint_showsByDefault_dismissPersists_andFirstFindClearsIt() = runBlocking {
        uiPreferences.onboardingMapHintSeen = false
        createActiveTrip()
        val vm = loadedViewModel()

        // First run: the tip is visible.
        assertTrue(vm.uiState.value.showMapHint)

        // Dismissing hides it and remembers across sessions.
        vm.onDismissMapHint()
        awaitUntil { !vm.uiState.value.showMapHint }
        assertTrue(uiPreferences.onboardingMapHintSeen)
    }

    @Test
    fun mapHint_isRetiredByTheFirstFind() = runBlocking {
        uiPreferences.onboardingMapHintSeen = false
        createActiveTrip()
        val vm = loadedViewModel()
        assertTrue(vm.uiState.value.showMapHint)

        spotting.markState(regions[0].regionCode)
        awaitUntil { vm.uiState.value.foundCount == 1 }
        awaitUntil { !vm.uiState.value.showMapHint }
        assertTrue(uiPreferences.onboardingMapHintSeen)
    }

    @Test
    fun rareCodes_arePopulatedFromRegionRarity() = runBlocking {
        // Make the first region a rare plate (same id, so this upsert updates it).
        db.plateRegionDao().upsertAll(listOf(regions[0].copy(rarityScore = 0.9)))
        createActiveTrip()
        val vm = loadedViewModel()

        awaitUntil { vm.uiState.value.rareCodes.contains(regions[0].regionCode) }
        assertTrue(regions[1].regionCode !in vm.uiState.value.rareCodes)
    }

    @Test
    fun firstEverCatch_emitsNewCollectionFlourish() = runBlocking {
        createActiveTrip()
        val vm = loadedViewModel()

        val events = mutableListOf<String>()
        val collector = CoroutineScope(Dispatchers.Main).launch {
            vm.newCollectionEvents.collect { events += it }
        }

        spotting.markState(regions[0].regionCode) // "S00", brand-new to the lifetime collection
        awaitUntil { events.contains(regions[0].name) }

        collector.cancel()
    }

    @Test
    fun routeCityPoints_projectGeocodedCities_andRouteStopsStillOrderStops() = runBlocking {
        cityLocator.coords["austin"] = GeoPoint(30.27, -97.74) // origin city
        createActiveTrip()
        val vm = loadedViewModel()

        // The ordered stop codes are still exposed for the line/centroid fallback.
        awaitUntil { vm.uiState.value.routeStops == listOf("S00", "S01") }
        // The origin city resolves to its projected map position.
        awaitUntil { vm.uiState.value.routeCityPoints.firstOrNull() != null }
        val p = vm.uiState.value.routeCityPoints.first()!!
        val expected = AlbersUsaProjection.project(30.27, -97.74)!!
        assertEquals(expected.x.toDouble(), p.x.toDouble(), 0.01)
        assertEquals(expected.y.toDouble(), p.y.toDouble(), 0.01)
        // The destination city wasn't geocodable, so it stays null (map falls back to state center).
        assertNull(vm.uiState.value.routeCityPoints[1])
    }

    @Test
    fun firstFind_logsAchievementUnlocked() = runBlocking {
        createActiveTrip()
        val analytics = FakeAnalytics()
        val vm = loadedViewModel(analytics)

        spotting.markState(regions[0].regionCode)
        awaitUntil { analytics.eventNames().contains("achievement_unlocked") }

        // The very first lifetime find unlocks "first_plate".
        val ids = analytics.events
            .filter { it.name == "achievement_unlocked" }
            .map { it.params["achievement_id"] }
        assertTrue("first_plate should unlock", ids.contains("first_plate"))
    }

    @Test
    fun onTabSelected_logsTabSelectedEvent() = runBlocking {
        createActiveTrip()
        val analytics = FakeAnalytics()
        val vm = loadedViewModel(analytics)

        vm.onTabSelected(ActiveTripTab.LIST)

        assertTrue(analytics.eventNames().contains("tab_selected"))
        assertEquals("list", analytics.paramsOf("tab_selected")?.get("tab"))
    }

    private fun loadedViewModel(analytics: FakeAnalytics = FakeAnalytics()): ActiveTripViewModel {
        val vm = ActiveTripViewModel(
            mapRepository = mapRepository,
            tripRepository = trips,
            spottingRepository = spotting,
            regionRepository = regionRepository,
            achievementRepository = achievements,
            cityLocator = cityLocator,
            celebrationTracker = celebrationTracker,
            uiPreferences = uiPreferences,
            settingsRepository = settings,
            analytics = analytics,
        )
        // Wait for the first pipeline emission (active trip loaded) so the celebration baseline
        // is established before we mark states.
        awaitUntil { !vm.uiState.value.loading && vm.uiState.value.tripId != null }
        return vm
    }

    private suspend fun createActiveTrip(): UUID = trips.createTrip(
        name = "Trip",
        originCity = "Austin",
        originRegionId = regions[0].id,
        destinationCity = "Denver",
        destinationRegionId = regions[1].id,
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
        assertTrue("Condition not met within ${timeoutMs}ms", condition())
    }

    /** In-memory geocoder: returns coords for cities placed in [coords] (keyed by lowercased name). */
    private class FakeCityLocator : CityLocator {
        val coords = mutableMapOf<String, GeoPoint>()
        override suspend fun locate(city: String, regionCode: String): GeoPoint? =
            coords[city.trim().lowercase()]
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
