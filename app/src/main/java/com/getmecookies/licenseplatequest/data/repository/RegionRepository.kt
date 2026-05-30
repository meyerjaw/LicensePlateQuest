package com.getmecookies.licenseplatequest.data.repository

import com.getmecookies.licenseplatequest.data.local.dao.PlateRegionDao
import com.getmecookies.licenseplatequest.data.local.entity.PlateRegionEntity
import com.getmecookies.licenseplatequest.domain.model.RegionOption
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Repository over bundled region data (SPEC section 9 — repository layer between ViewModels
 * and DAOs). Read-only from the app's perspective; writes happen only via the seeder.
 */
class RegionRepository(
    private val plateRegionDao: PlateRegionDao,
) {
    fun observeRegionCount(): Flow<Int> = plateRegionDao.observeCount()

    suspend fun getAllRegions(): List<PlateRegionEntity> = plateRegionDao.getAll()

    /** Region options for the trip-creation state dropdowns. */
    fun observeRegionOptions(): Flow<List<RegionOption>> =
        plateRegionDao.observeAll().map { regions ->
            regions.map { RegionOption(id = it.id, code = it.regionCode, name = it.name) }
        }
}
