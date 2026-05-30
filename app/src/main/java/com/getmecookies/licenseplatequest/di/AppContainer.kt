package com.getmecookies.licenseplatequest.di

import android.content.Context
import com.getmecookies.licenseplatequest.data.local.AppDatabase
import com.getmecookies.licenseplatequest.data.local.DatabaseProvider
import com.getmecookies.licenseplatequest.data.repository.RegionRepository
import com.getmecookies.licenseplatequest.data.seed.RegionSeeder

/**
 * Manual dependency container (MVP uses no DI framework, per SPEC §9). Owns the database
 * and exposes repositories + the seeder. Held by the Application and reached from
 * ViewModel factories via the application instance.
 */
class AppContainer(context: Context) {

    val database: AppDatabase = DatabaseProvider.get(context)

    val regionRepository: RegionRepository = RegionRepository(database.plateRegionDao())

    val regionSeeder: RegionSeeder = RegionSeeder(
        context = context.applicationContext,
        plateRegionDao = database.plateRegionDao(),
        gameTypeDao = database.gameTypeDao(),
    )
}
