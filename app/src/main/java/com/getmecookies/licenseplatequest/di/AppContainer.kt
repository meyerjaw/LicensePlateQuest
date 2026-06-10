package com.getmecookies.licenseplatequest.di

import android.content.Context
import com.getmecookies.licenseplatequest.data.local.AppDatabase
import com.getmecookies.licenseplatequest.data.local.DatabaseProvider
import com.getmecookies.licenseplatequest.data.map.MapRepository
import com.getmecookies.licenseplatequest.data.repository.AchievementRepository
import com.getmecookies.licenseplatequest.data.repository.CelebrationRepository
import com.getmecookies.licenseplatequest.data.repository.PlayerRepository
import com.getmecookies.licenseplatequest.data.repository.RegionRepository
import com.getmecookies.licenseplatequest.data.repository.SettingsRepository
import com.getmecookies.licenseplatequest.data.repository.SpottingRepository
import com.getmecookies.licenseplatequest.data.repository.TripRepository
import com.getmecookies.licenseplatequest.data.seed.RegionSeeder
import com.getmecookies.licenseplatequest.data.seed.SampleDataSeeder
import com.getmecookies.licenseplatequest.domain.CelebrationTracker
import com.getmecookies.licenseplatequest.domain.UiPreferences
import com.getmecookies.licenseplatequest.notifications.ReminderScheduler
import com.getmecookies.licenseplatequest.notifications.WorkManagerReminderScheduler

/**
 * Manual dependency container (MVP uses no DI framework, per SPEC section 9). Owns the
 * database and exposes repositories + the seeder. Held by the Application and reached from
 * ViewModel factories via the application instance.
 */
class AppContainer(context: Context) {

    val database: AppDatabase = DatabaseProvider.get(context)

    val regionRepository: RegionRepository = RegionRepository(database.plateRegionDao())

    val playerRepository: PlayerRepository = PlayerRepository(
        playerDao = database.playerDao(),
        tripPlayerDao = database.tripPlayerDao(),
        eventLogDao = database.eventLogDao(),
    )

    val reminderScheduler: ReminderScheduler = WorkManagerReminderScheduler(context.applicationContext)

    val tripRepository: TripRepository = TripRepository(database, reminderScheduler)

    val spottingRepository: SpottingRepository = SpottingRepository(database)

    val achievementRepository: AchievementRepository =
        AchievementRepository(database, regionRepository)

    val celebrationRepository: CelebrationRepository = CelebrationRepository(database)

    val celebrationTracker: CelebrationTracker = CelebrationTracker(context.applicationContext)

    val uiPreferences: UiPreferences = UiPreferences(context.applicationContext)

    val settingsRepository: SettingsRepository = SettingsRepository(context.applicationContext)

    val mapRepository: MapRepository = MapRepository(context.applicationContext)

    val regionSeeder: RegionSeeder = RegionSeeder(
        context = context.applicationContext,
        plateRegionDao = database.plateRegionDao(),
        gameTypeDao = database.gameTypeDao(),
    )

    val sampleDataSeeder: SampleDataSeeder = SampleDataSeeder(
        database = database,
        regionRepository = regionRepository,
        playerRepository = playerRepository,
        tripRepository = tripRepository,
        spottingRepository = spottingRepository,
        achievementRepository = achievementRepository,
        regionSeeder = regionSeeder,
    )
}
