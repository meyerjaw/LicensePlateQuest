package com.getmecookies.licenseplatequest.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.getmecookies.licenseplatequest.data.local.entity.GameInstanceEntity
import java.util.UUID

@Dao
interface GameInstanceDao {

    @Insert
    suspend fun insert(gameInstance: GameInstanceEntity)

    @Query("SELECT * FROM game_instance WHERE trip_id = :tripId")
    suspend fun getForTrip(tripId: UUID): List<GameInstanceEntity>

    /** All game instances (backup export). */
    @Query("SELECT * FROM game_instance")
    suspend fun getAll(): List<GameInstanceEntity>

    /** Bulk insert, skipping rows whose primary key already exists (backup import / merge). */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAllIgnore(gameInstances: List<GameInstanceEntity>)
}
