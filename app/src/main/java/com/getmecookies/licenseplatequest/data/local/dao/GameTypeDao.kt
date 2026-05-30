package com.getmecookies.licenseplatequest.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.getmecookies.licenseplatequest.data.local.entity.GameTypeEntity

@Dao
interface GameTypeDao {

    @Upsert
    suspend fun upsert(gameType: GameTypeEntity)

    @Query("SELECT * FROM game_type WHERE code = :code LIMIT 1")
    suspend fun getByCode(code: String): GameTypeEntity?
}
