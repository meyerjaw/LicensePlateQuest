package com.getmecookies.licenseplatequest.ui.screens.onboarding

import android.content.Context
import android.os.Looper
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.getmecookies.licenseplatequest.data.local.AppDatabase
import com.getmecookies.licenseplatequest.data.local.entity.GameTypeEntity
import com.getmecookies.licenseplatequest.data.local.entity.PlateRegionEntity
import com.getmecookies.licenseplatequest.data.repository.PlayerRepository
import com.getmecookies.licenseplatequest.data.repository.RegionRepository
import com.getmecookies.licenseplatequest.data.repository.SettingsRepository
import com.getmecookies.licenseplatequest.data.repository.TripRepository
import com.getmecookies.licenseplatequest.data.seed.RegionSeeder
import com.getmecookies.licenseplatequest.domain.FakeAnalytics
import com.getmecookies.licenseplatequest.domain.UiPreferences
import com.getmecookies.licenseplatequest.notifications.FakeReminderScheduler
import com.getmecookies.licenseplatequest.testutil.MainDispatcherRule
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
import java.util.UUID

/** Orchestration coverage for the onboarding wizard ViewModel (step nav, validation, completion). */
@RunWith(RobolectricTestRunner::class)
class OnboardingViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var db: AppDatabase
    private lateinit var uiPreferences: UiPreferences
    private lateinit var settings: SettingsRepository
    private lateinit var players: PlayerRepository
    private lateinit var trips: TripRepository
    private lateinit var regionRepository: RegionRepository
    private lateinit var regions: List<PlateRegionEntity>

    @Before
    fun setUp() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        uiPreferences = UiPreferences(context)
        settings = SettingsRepository(context)
        players = PlayerRepository(db.playerDao(), db.tripPlayerDao(), db.eventLogDao())
        trips = TripRepository(db, FakeReminderScheduler())
        regionRepository = RegionRepository(db.plateRegionDao())

        regions = (0 until 3).map { region("S%02d".format(it), it) }
        db.plateRegionDao().upsertAll(regions)
        db.gameTypeDao().upsert(
            GameTypeEntity(UUID.randomUUID(), RegionSeeder.LICENSE_PLATE_CODE, "License Plate", ""),
        )
        // Start from a clean first-run state.
        uiPreferences.onboardingStep = 0
        uiPreferences.setOnboardingComplete(false)
    }

    @After
    fun tearDown() = db.close()

    private fun viewModel(analytics: FakeAnalytics = FakeAnalytics()) =
        OnboardingViewModel(uiPreferences, settings, players, trips, regionRepository, analytics)

    @Test
    fun nextAndBack_moveStepAndPersist() {
        val vm = viewModel()
        assertEquals(0, vm.uiState.value.step)
        vm.next()
        assertEquals(1, vm.uiState.value.step)
        assertEquals(1, uiPreferences.onboardingStep)
        vm.back()
        assertEquals(0, vm.uiState.value.step)
        assertEquals(0, uiPreferences.onboardingStep)
    }

    @Test
    fun addPlayer_appendsAndClearsDraft() {
        val vm = viewModel()
        vm.onPlayerNameDraft("Alice")
        vm.addPlayer()
        awaitUntil { vm.uiState.value.players.size == 1 }
        val state = vm.uiState.value
        assertEquals("Alice", state.players.first().name)
        assertEquals("", state.playerNameDraft)
        // A blank draft is ignored.
        vm.addPlayer()
        assertEquals(1, vm.uiState.value.players.size)
    }

    @Test
    fun autoNamesTripFromRoute_untilEdited() {
        val vm = viewModel()
        awaitUntil { vm.uiState.value.regionOptions.size >= 2 }
        vm.onOriginCity("Austin")
        vm.onDestRegion(regions[1].id) // code "S01"
        vm.onDestCity("Denver")
        // Same rule as New Trip: "Origin to Destination, CODE - Month Year".
        assertTrue(vm.uiState.value.tripName.startsWith("Austin to Denver, S01 - "))
        // A manual edit takes over; later route edits don't clobber it.
        vm.onTripName("Spring Break")
        vm.onDestCity("Boulder")
        assertEquals("Spring Break", vm.uiState.value.tripName)
    }

    @Test
    fun createTrip_requiresValidFormThenAdvances() {
        val vm = viewModel()
        // Invalid form → no-op (no advance).
        assertFalse(vm.uiState.value.tripValid)
        vm.createTripAndNext()
        assertEquals(0, vm.uiState.value.step)

        // Walk to the trip step (index 3) and fill it in.
        vm.next(); vm.next(); vm.next()
        assertEquals(3, vm.uiState.value.step)
        vm.onTripName("Beach")
        vm.onOriginRegion(regions[0].id)
        vm.onOriginCity("Austin")
        vm.onDestRegion(regions[1].id)
        vm.onDestCity("Dallas")
        assertTrue(vm.uiState.value.tripValid)
        vm.createTripAndNext()
        awaitUntil { vm.uiState.value.step == 4 }
    }

    @Test
    fun finish_flipsCompletionFlag() {
        val vm = viewModel()
        vm.finish()
        assertTrue(uiPreferences.onboardingComplete.value)
        assertEquals(0, uiPreferences.onboardingStep)
    }

    @Test
    fun finishFromWelcome_logsOnboardingSkipped() {
        val analytics = FakeAnalytics()
        val vm = viewModel(analytics) // starts at step 0 (Welcome)

        vm.finish()

        assertEquals("onboarding_skipped", analytics.eventNames().last())
        assertEquals(0, analytics.paramsOf("onboarding_skipped")?.get("step"))
    }

    @Test
    fun finishFromReadyStep_logsOnboardingCompleted() {
        val analytics = FakeAnalytics()
        val vm = viewModel(analytics)
        repeat(OnboardingViewModel.LAST_STEP) { vm.next() } // advance to the Ready step

        vm.finish()

        assertEquals("onboarding_completed", analytics.eventNames().last())
        assertEquals(
            OnboardingViewModel.LAST_STEP,
            analytics.paramsOf("onboarding_completed")?.get("step"),
        )
    }

    /** Spin the main looper until [condition] holds (the VM writes through real Room off-thread). */
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
