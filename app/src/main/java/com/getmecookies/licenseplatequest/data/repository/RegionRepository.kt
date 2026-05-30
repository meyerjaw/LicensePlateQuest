package com.getmecookies.licenseplatequest.data.repository

import com.getmecookies.licenseplatequest.data.local.dao.PlateRegionDao
import com.getmecookies.licenseplatequest.data.local.entity.PlateRegionEntity
import kotlinx.coroutines.flow.Flow

/**
 * Repository over bundled region data (SPEC §9 — repository layer between ViewModels and
 * DAOs). Read-only from the app's perspective; writes happen only via the seeder.
 */
class RegionRepository(
    private val plateRegionDao: PlateRegionDao,
) {
    fun observeRegionCount(): Flow<Int> = plateRegionDao.observeCount()

    suspend fun getAllRegions(): List<PlateRegionEntity> = plateRegionDao.getAll()
}
