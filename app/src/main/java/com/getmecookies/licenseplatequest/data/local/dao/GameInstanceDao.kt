package com.getmecookies.licenseplatequest.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.getmecookies.licenseplatequest.data.local.entity.GameInstanceEntity
import java.util.UUID

@Dao
interface GameInstanceDao {

    @Insert
    suspend fun insert(gameInstance: GameInstanceEntity)

    @Query("SELECT * FROM game_instance WHERE trip_id = :tripId")
    suspend fun getForTrip(tripId: UUID): List<GameInstanceEntity>
}
