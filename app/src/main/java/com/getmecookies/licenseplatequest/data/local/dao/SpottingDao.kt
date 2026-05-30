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
}
