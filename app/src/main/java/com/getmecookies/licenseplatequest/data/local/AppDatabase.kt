package com.getmecookies.licenseplatequest.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.getmecookies.licenseplatequest.data.local.AppDatabase.Companion.VERSION
import com.getmecookies.licenseplatequest.data.local.dao.EventLogDao
import com.getmecookies.licenseplatequest.data.local.dao.GameInstanceDao
import com.getmecookies.licenseplatequest.data.local.dao.GameTypeDao
import com.getmecookies.licenseplatequest.data.local.dao.PlateRegionDao
import com.getmecookies.licenseplatequest.data.local.dao.PlayerDao
import com.getmecookies.licenseplatequest.data.local.dao.SpottingDao
import com.getmecookies.licenseplatequest.data.local.dao.SpottingPlayerDao
import com.getmecookies.licenseplatequest.data.local.dao.TripDao
import com.getmecookies.licenseplatequest.data.local.dao.TripPlayerDao
import com.getmecookies.licenseplatequest.data.local.entity.EventLogEntity
import com.getmecookies.licenseplatequest.data.local.entity.GameInstanceEntity
import com.getmecookies.licenseplatequest.data.local.entity.GameTypeEntity
import com.getmecookies.licenseplatequest.data.local.entity.PlateRegionEntity
import com.getmecookies.licenseplatequest.data.local.entity.PlayerEntity
import com.getmecookies.licenseplatequest.data.local.entity.SpottingEntity
import com.getmecookies.licenseplatequest.data.local.entity.SpottingPlayerEntity
import com.getmecookies.licenseplatequest.data.local.entity.TripEntity
import com.getmecookies.licenseplatequest.data.local.entity.TripPlayerEntity

/**
 * Single Room database for all on-device data (SPEC §9 — no backend).
 *
 * Schema is exported (see `room.schemaLocation` in app/build.gradle.kts) so that from v1
 * onward every schema change ships with an explicit [androidx.room.migration.Migration].
 * Bump [VERSION] and add a migration to [DatabaseProvider] when entities change — there is
 * deliberately no destructive fallback, to protect real trip data.
 */
@Database(
    entities = [
        PlayerEntity::class,
        PlateRegionEntity::class,
        TripEntity::class,
        TripPlayerEntity::class,
        GameTypeEntity::class,
        GameInstanceEntity::class,
        SpottingEntity::class,
        SpottingPlayerEntity::class,
        EventLogEntity::class,
    ],
    version = AppDatabase.VERSION,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun playerDao(): PlayerDao
    abstract fun plateRegionDao(): PlateRegionDao
    abstract fun tripDao(): TripDao
    abstract fun tripPlayerDao(): TripPlayerDao
    abstract fun gameTypeDao(): GameTypeDao
    abstract fun gameInstanceDao(): GameInstanceDao
    abstract fun spottingDao(): SpottingDao
    abstract fun spottingPlayerDao(): SpottingPlayerDao
    abstract fun eventLogDao(): EventLogDao

    companion object {
        const val VERSION = 3
        const val NAME = "license_plate_quest.db"
    }
}

/**
 * v1 → v2 (playtest notes #17/#19): add the player [PlayerEntity.color] column and create the
 * `spotting_player` junction for multi-player attribution. DDL mirrors Room's generated schema
 * exactly (UUIDs stored as TEXT) so the validator accepts it.
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `player` ADD COLUMN `color` TEXT")
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `spotting_player` (" +
                "`id` TEXT NOT NULL, " +
                "`spotting_id` TEXT NOT NULL, " +
                "`player_id` TEXT NOT NULL, " +
                "PRIMARY KEY(`id`), " +
                "FOREIGN KEY(`spotting_id`) REFERENCES `spotting`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE, " +
                "FOREIGN KEY(`player_id`) REFERENCES `player`(`id`) ON UPDATE NO ACTION ON DELETE NO ACTION)",
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_spotting_player_spotting_id` ON `spotting_player` (`spotting_id`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_spotting_player_player_id` ON `spotting_player` (`player_id`)")
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_spotting_player_spotting_id_player_id` " +
                "ON `spotting_player` (`spotting_id`, `player_id`)",
        )
    }
}

/**
 * v2 → v3 (playtest note #12): add the optional trip [TripEntity.endDate]. Stored as an ISO-8601
 * date string (nullable), matching Room's generated schema.
 */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `trip` ADD COLUMN `end_date` TEXT")
    }
}

/**
 * Process-wide singleton holder for [AppDatabase]. Manual DI (no Hilt in MVP) — the single
 * instance is created lazily and shared via [com.getmecookies.licenseplatequest.di.AppContainer].
 */
object DatabaseProvider {

    @Volatile
    private var instance: AppDatabase? = null

    fun get(context: Context): AppDatabase =
        instance ?: synchronized(this) {
            instance ?: build(context).also { instance = it }
        }

    private fun build(context: Context): AppDatabase =
        Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            AppDatabase.NAME,
        )
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
            .build()
}
