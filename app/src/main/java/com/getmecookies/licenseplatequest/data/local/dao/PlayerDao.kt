package com.getmecookies.licenseplatequest.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.getmecookies.licenseplatequest.data.local.entity.PlayerEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Dao
interface PlayerDao {

    @Insert
    suspend fun insert(player: PlayerEntity)

    @Update
    suspend fun update(player: PlayerEntity)

    /** Active (non-soft-deleted) players, case-insensitively by name. */
    @Query("SELECT * FROM player WHERE deleted = 0 ORDER BY name COLLATE NOCASE")
    fun observeActive(): Flow<List<PlayerEntity>>

    @Query("SELECT * FROM player WHERE id = :id")
    suspend fun getById(id: UUID): PlayerEntity?
}
