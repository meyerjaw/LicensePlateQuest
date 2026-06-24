package com.getmecookies.licenseplatequest.data.backup

import androidx.room.withTransaction
import com.getmecookies.licenseplatequest.data.local.AppDatabase
import com.getmecookies.licenseplatequest.data.repository.SettingsRepository
import com.getmecookies.licenseplatequest.domain.model.ThemeMode
import kotlinx.serialization.json.Json
import java.time.Instant
import java.util.UUID

/**
 * Local export/import of all user data (basic backup — distinct from any future online sync).
 *
 * Export reads every user-data table + settings into a [BackupFile]; import writes them back under
 * one of two [ImportMode]s. Reference data (regions, game types) is never touched — it's reseeded
 * with deterministic ids, so foreign keys resolve after a restore on any device (see [BackupModels]).
 */
class BackupRepository(
    private val database: AppDatabase,
    private val settings: SettingsRepository,
) {
    /** How an import reconciles with existing data. */
    enum class ImportMode {
        /** Wipe current user data first, then load the backup (a clean restore). */
        REPLACE,

        /** Add backup rows on top of current data, skipping ids that already exist. */
        MERGE,
    }

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    // ---- Export ---------------------------------------------------------------------------------

    /** Read everything into a [BackupFile]. */
    suspend fun export(): BackupFile = database.withTransaction {
        BackupFile(
            appDbVersion = AppDatabase.VERSION,
            exportedAt = Instant.now().toString(),
            data = BackupData(
                players = database.playerDao().getAll().map { it.toBackup() },
                trips = database.tripDao().getAll().map { it.toBackup() },
                tripPlayers = database.tripPlayerDao().getAll().map { it.toBackup() },
                tripStops = database.tripStopDao().getAll().map { it.toBackup() },
                gameInstances = database.gameInstanceDao().getAll().map { it.toBackup() },
                spottings = database.spottingDao().getAll().map { it.toBackup() },
                spottingPlayers = database.spottingPlayerDao().getAll().map { it.toBackup() },
                achievements = database.achievementDao().getAll().map { it.toBackup() },
                eventLog = database.eventLogDao().getAll().map { it.toBackup() },
                settings = currentSettings(),
            ),
        )
    }

    /** Export and serialize to pretty JSON (what the export file contains). */
    suspend fun exportToJson(): String = json.encodeToString(BackupFile.serializer(), export())

    // ---- Import ---------------------------------------------------------------------------------

    /**
     * Parse [text] and import it. Throws [IllegalArgumentException] if the file is unreadable or from
     * a newer app version than this build supports.
     */
    suspend fun importFromJson(text: String, mode: ImportMode) {
        val file = try {
            json.decodeFromString(BackupFile.serializer(), text)
        } catch (e: Exception) {
            throw IllegalArgumentException("This file isn't a valid License Plate Quest backup.", e)
        }
        import(file, mode)
    }

    /** Import an already-parsed [file]. Reference data is left intact; settings apply on REPLACE. */
    suspend fun import(file: BackupFile, mode: ImportMode) {
        require(file.appDbVersion <= AppDatabase.VERSION) {
            "This backup is from a newer version of the app and can't be imported here."
        }
        database.withTransaction {
            if (mode == ImportMode.REPLACE) wipeUserData()
            val d = file.data
            // Insert in FK-safe order: parents before children.
            database.playerDao().insertAllIgnore(d.players.map { it.toEntity() })
            database.tripDao().insertAllIgnore(d.trips.map { it.toEntity() })
            database.tripPlayerDao().insertAllIgnore(d.tripPlayers.map { it.toEntity() })
            database.tripStopDao().insertAllIgnore(d.tripStops.map { it.toEntity() })
            database.gameInstanceDao().insertAllIgnore(d.gameInstances.map { it.toEntity() })
            database.spottingDao().insertAllIgnore(d.spottings.map { it.toEntity() })
            database.spottingPlayerDao().insertAllIgnore(d.spottingPlayers.map { it.toEntity() })
            database.achievementDao().insertIgnore(d.achievements.map { it.toEntity() })
            database.eventLogDao().insertAllIgnore(d.eventLog.map { it.toEntity() })
        }
        // Settings live in SharedPreferences, not the DB. Only a REPLACE (full restore) overwrites
        // them; a MERGE keeps this device's settings and just adds gameplay data.
        if (mode == ImportMode.REPLACE) applySettings(file.data.settings)
    }

    private suspend fun wipeUserData() {
        // Deleting trips FK-cascades to stops, trip_players, game_instances, spottings, and
        // spotting_players; players/achievements/event_log are independent.
        database.tripDao().deleteAll()
        database.playerDao().deleteAll()
        database.achievementDao().deleteAll()
        database.eventLogDao().deleteAll()
    }

    // ---- Settings -------------------------------------------------------------------------------

    private fun currentSettings(): SettingsBackup {
        val home = settings.home.value
        return SettingsBackup(
            themeMode = settings.themeMode.value.name,
            hapticsEnabled = settings.hapticsEnabled.value,
            soundEnabled = settings.soundEnabled.value,
            tripRemindersEnabled = settings.tripRemindersEnabled.value,
            analyticsEnabled = settings.analyticsEnabled.value,
            homeRegionId = home?.regionId?.toString(),
            homeCity = home?.city,
        )
    }

    private fun applySettings(s: SettingsBackup) {
        s.themeMode?.let { name ->
            runCatching { ThemeMode.valueOf(name) }.getOrNull()?.let(settings::setThemeMode)
        }
        s.hapticsEnabled?.let(settings::setHapticsEnabled)
        s.soundEnabled?.let(settings::setSoundEnabled)
        s.tripRemindersEnabled?.let(settings::setTripRemindersEnabled)
        s.analyticsEnabled?.let(settings::setAnalyticsEnabled)
        val regionId = s.homeRegionId?.let { runCatching { UUID.fromString(it) }.getOrNull() }
        if (regionId != null && !s.homeCity.isNullOrBlank()) {
            settings.setHome(regionId, s.homeCity)
        } else {
            settings.clearHome()
        }
    }
}
