package com.getmecookies.licenseplatequest.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.getmecookies.licenseplatequest.data.local.dao.EventLogDao
import com.getmecookies.licenseplatequest.data.local.dao.GameInstanceDao
import com.getmecookies.licenseplatequest.data.local.dao.GameTypeDao
import com.getmecookies.licenseplatequest.data.local.dao.PlateRegionDao
import com.getmecookies.licenseplatequest.data.local.dao.PlayerDao
import com.getmecookies.licenseplatequest.data.local.dao.SpottingDao
import com.getmecookies.licenseplatequest.data.local.dao.TripDao
import com.getmecookies.licenseplatequest.data.local.dao.TripPlayerDao
import com.getmecookies.licenseplatequest.data.local.entity.EventLogEntity
import com.getmecookies.licenseplatequest.data.local.entity.GameInstanceEntity
import com.getmecookies.licenseplatequest.data.local.entity.GameTypeEntity
import com.getmecookies.licenseplatequest.data.local.entity.PlateRegionEntity
import com.getmecookies.licenseplatequest.data.local.entity.PlayerEntity
import com.getmecookies.licenseplatequest.data.local.entity.SpottingEntity
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
    abstract fun eventLogDao(): EventLogDao

    companion object {
        const val VERSION = 1
        const val NAME = "license_plate_quest.db"
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
            // Future schema bumps: .addMigrations(MIGRATION_1_2, ...)
            .build()
}
