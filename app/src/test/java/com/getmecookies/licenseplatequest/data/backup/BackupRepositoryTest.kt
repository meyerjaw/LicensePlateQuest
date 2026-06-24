package com.getmecookies.licenseplatequest.data.backup

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.getmecookies.licenseplatequest.data.local.AppDatabase
import com.getmecookies.licenseplatequest.data.local.entity.AchievementEntity
import com.getmecookies.licenseplatequest.data.local.entity.EventLogEntity
import com.getmecookies.licenseplatequest.data.local.entity.GameInstanceEntity
import com.getmecookies.licenseplatequest.data.local.entity.GameTypeEntity
import com.getmecookies.licenseplatequest.data.local.entity.PlateRegionEntity
import com.getmecookies.licenseplatequest.data.local.entity.PlayerEntity
import com.getmecookies.licenseplatequest.data.local.entity.SpottingEntity
import com.getmecookies.licenseplatequest.data.local.entity.SpottingPlayerEntity
import com.getmecookies.licenseplatequest.data.local.entity.TripEntity
import com.getmecookies.licenseplatequest.data.local.entity.TripPlayerEntity
import com.getmecookies.licenseplatequest.data.local.entity.TripStopEntity
import com.getmecookies.licenseplatequest.data.repository.SettingsRepository
import com.getmecookies.licenseplatequest.data.seed.Ids
import com.getmecookies.licenseplatequest.data.seed.RegionSeeder
import com.getmecookies.licenseplatequest.domain.model.ThemeMode
import com.getmecookies.licenseplatequest.domain.model.TripStatus
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

/**
 * Round-trips the backup through the real DAOs (in-memory Room). The dataset spans every exported
 * table so a faithful restore is verified end to end, plus the merge-dedupe and version-guard rules.
 */
@RunWith(RobolectricTestRunner::class)
class BackupRepositoryTest {

    private lateinit var db: AppDatabase
    private lateinit var settings: SettingsRepository
    private lateinit var backup: BackupRepository

    private val region0 = Ids.region("US", "S00")
    private val region1 = Ids.region("US", "S01")
    private val gameTypeId = Ids.gameType(RegionSeeder.LICENSE_PLATE_CODE)
    private val playerId = UUID.randomUUID()
    private val tripId = UUID.randomUUID()
    private val gameInstanceId = UUID.randomUUID()
    private val spottingId = UUID.randomUUID()
    private val now = Instant.parse("2026-06-14T12:00:00Z")

    @Before
    fun setUp() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        settings = SettingsRepository(context)
        backup = BackupRepository(db, settings)
        seedReferenceData()
        Unit
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun replaceImport_restoresEveryTableAndSettings_fromTheFileAlone() = runBlocking {
        buildFullDataset()
        val json = backup.exportToJson()

        // Mutate the live DB + settings so a successful restore must come from the file.
        db.tripDao().deleteAll()
        db.playerDao().deleteAll()
        db.achievementDao().deleteAll()
        db.eventLogDao().deleteAll()
        settings.setThemeMode(ThemeMode.SYSTEM)
        settings.setSoundEnabled(true)
        settings.clearHome()

        backup.importFromJson(json, BackupRepository.ImportMode.REPLACE)

        assertEquals(listOf(playerId), db.playerDao().getAll().map { it.id })
        assertEquals(listOf(tripId), db.tripDao().getAll().map { it.id })
        assertEquals(1, db.tripPlayerDao().getAll().size)
        assertEquals(2, db.tripStopDao().getAll().size)
        assertEquals(listOf(gameInstanceId), db.gameInstanceDao().getAll().map { it.id })
        assertEquals(listOf(spottingId), db.spottingDao().getAll().map { it.id })
        assertEquals(1, db.spottingPlayerDao().getAll().size)
        assertEquals(listOf("first_plate"), db.achievementDao().getAll().map { it.id })
        assertEquals(1, db.eventLogDao().getAll().size)

        // Settings restored from the backup.
        assertEquals(ThemeMode.DARK, settings.themeMode.value)
        assertEquals(false, settings.soundEnabled.value)
        assertEquals(region0, settings.home.value?.regionId)
        assertEquals("Austin", settings.home.value?.city)
    }

    @Test
    fun mergeImport_dedupesExisting_andAddsNewRows() = runBlocking {
        buildFullDataset()

        // Re-importing the current data as a MERGE changes nothing (all ids already exist).
        backup.import(backup.export(), BackupRepository.ImportMode.MERGE)
        assertEquals(1, db.playerDao().getAll().size)

        // A backup carrying one brand-new player merges that player in, keeping the existing one.
        val newPlayerId = UUID.randomUUID()
        val withExtra = backup.export().let { file ->
            file.copy(
                data = file.data.copy(
                    players = file.data.players + PlayerBackup(
                        id = newPlayerId.toString(),
                        name = "New Person",
                        createdAt = now.toString(),
                        updatedAt = now.toString(),
                    ),
                ),
            )
        }
        backup.import(withExtra, BackupRepository.ImportMode.MERGE)

        val ids = db.playerDao().getAll().map { it.id }
        assertEquals(2, ids.size)
        assertTrue(ids.containsAll(listOf(playerId, newPlayerId)))
    }

    @Test
    fun import_rejectsBackupFromNewerAppVersion() {
        val tooNew = BackupFile(
            appDbVersion = AppDatabase.VERSION + 1,
            exportedAt = now.toString(),
            data = BackupData(),
        )
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { backup.import(tooNew, BackupRepository.ImportMode.REPLACE) }
        }
    }

    // ---- Fixtures -------------------------------------------------------------------------------

    private suspend fun seedReferenceData() {
        db.plateRegionDao().upsertAll(listOf(region("S00", region0, 0), region("S01", region1, 1)))
        db.gameTypeDao().upsert(
            GameTypeEntity(gameTypeId, RegionSeeder.LICENSE_PLATE_CODE, "License Plate", ""),
        )
    }

    private suspend fun buildFullDataset() {
        db.playerDao().insert(PlayerEntity(playerId, "Sam", now, now, deleted = false, color = "teal"))
        db.tripDao().insert(
            TripEntity(
                id = tripId,
                name = "Trip",
                originCity = "Austin",
                originRegionId = region0,
                destinationCity = "Denver",
                destinationRegionId = region1,
                startDate = LocalDate.parse("2026-06-01"),
                endDate = null,
                status = TripStatus.ACTIVE,
                endedAt = null,
                createdAt = now,
                updatedAt = now,
            ),
        )
        db.tripPlayerDao().insert(TripPlayerEntity(UUID.randomUUID(), tripId, playerId, now))
        db.tripStopDao().insertAll(
            listOf(
                TripStopEntity(UUID.randomUUID(), tripId, 0, region0, "Austin"),
                TripStopEntity(UUID.randomUUID(), tripId, 1, region1, "Denver"),
            ),
        )
        db.gameInstanceDao().insert(GameInstanceEntity(gameInstanceId, tripId, gameTypeId, now))
        db.spottingDao().insert(
            SpottingEntity(
                id = spottingId,
                gameInstanceId = gameInstanceId,
                plateRegionId = region0,
                spotterPlayerId = null,
                timestamp = now,
                note = null,
                photoPath = null,
                gpsLat = null,
                gpsLng = null,
                createdAt = now,
                celebratedAt = null,
            ),
        )
        db.spottingPlayerDao().insert(SpottingPlayerEntity(UUID.randomUUID(), spottingId, playerId))
        db.achievementDao().insertIgnore(listOf(AchievementEntity("first_plate", now)))
        db.eventLogDao().insert(EventLogEntity(UUID.randomUUID(), "state_found", "{}", now))

        settings.setThemeMode(ThemeMode.DARK)
        settings.setSoundEnabled(false)
        settings.setHome(region0, "Austin")
    }

    private fun region(code: String, id: UUID, order: Int) = PlateRegionEntity(
        id = id,
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
