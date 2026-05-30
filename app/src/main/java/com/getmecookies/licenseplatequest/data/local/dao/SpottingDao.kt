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
}
