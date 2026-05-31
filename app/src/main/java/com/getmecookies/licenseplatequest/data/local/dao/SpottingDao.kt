package com.getmecookies.licenseplatequest.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.getmecookies.licenseplatequest.data.local.entity.SpottingEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Dao
interface SpottingDao {

    @Insert
    suspend fun insert(spotting: SpottingEntity)

    @Delete
    suspend fun delete(spotting: SpottingEntity)

    @Query("SELECT * FROM spotting WHERE game_instance_id = :gameInstanceId ORDER BY timestamp")
    fun observeForGame(gameInstanceId: UUID): Flow<List<SpottingEntity>>

    /** The 2-letter region codes found in a game, for coloring the map. */
    @Query(
        """
        SELECT pr.region_code
        FROM spotting s
        JOIN plate_region pr ON pr.id = s.plate_region_id
        WHERE s.game_instance_id = :gameInstanceId
        """
    )
    fun observeFoundCodesForGame(gameInstanceId: UUID): Flow<List<String>>

    /** The spotting for a given region in a game, if it's been marked. */
    @Query(
        "SELECT * FROM spotting WHERE game_instance_id = :gameInstanceId " +
            "AND plate_region_id = :plateRegionId LIMIT 1"
    )
    suspend fun getForRegion(gameInstanceId: UUID, plateRegionId: UUID): SpottingEntity?

    /**
     * Found states in a game with the details the Active Trip View's bottom sheet needs,
     * newest-found first. Ordering by the ISO-8601 UTC timestamp string sorts chronologically.
     */
    @Query(
        """
        SELECT pr.region_code AS region_code,
               pr.name AS name,
               pr.plate_image_path AS plate_image_path,
               s.timestamp AS found_at
        FROM spotting s
        JOIN plate_region pr ON pr.id = s.plate_region_id
        WHERE s.game_instance_id = :gameInstanceId
        ORDER BY s.timestamp DESC
        """
    )
    fun observeFoundDetailsForGame(gameInstanceId: UUID): Flow<List<FoundStateRow>>

    /**
     * All spottings in a game with the region attributes needed to compute celebration stats
     * (name, geographic center, rarity), ordered chronologically.
     */
    @Query(
        """
        SELECT pr.name AS name,
               pr.center_lat AS center_lat,
               pr.center_lng AS center_lng,
               pr.rarity_score AS rarity_score,
               s.timestamp AS timestamp
        FROM spotting s
        JOIN plate_region pr ON pr.id = s.plate_region_id
        WHERE s.game_instance_id = :gameInstanceId
        ORDER BY s.timestamp
        """
    )
    suspend fun getStatRows(gameInstanceId: UUID): List<SpottingStatRow>
}
