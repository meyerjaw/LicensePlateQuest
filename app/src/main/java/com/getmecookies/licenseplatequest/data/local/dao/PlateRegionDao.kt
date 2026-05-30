package com.getmecookies.licenseplatequest.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.getmecookies.licenseplatequest.data.local.entity.PlateRegionEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Dao
interface PlateRegionDao {

    /** Insert-or-update by primary key. Avoids REPLACE so dependent FK rows aren't cascaded. */
    @Upsert
    suspend fun upsertAll(regions: List<PlateRegionEntity>)

    @Query("SELECT COUNT(*) FROM plate_region")
    fun observeCount(): Flow<Int>

    @Query("SELECT * FROM plate_region ORDER BY display_order")
    suspend fun getAll(): List<PlateRegionEntity>

    /** Observe regions (ordered) so dropdowns populate once first-run seeding completes. */
    @Query("SELECT * FROM plate_region ORDER BY display_order")
    fun observeAll(): Flow<List<PlateRegionEntity>>

    @Query("SELECT * FROM plate_region WHERE country_code = :country AND region_code = :region LIMIT 1")
    suspend fun getByCode(country: String, region: String): PlateRegionEntity?

    @Query("SELECT * FROM plate_region WHERE id = :id")
    suspend fun getById(id: UUID): PlateRegionEntity?
}
