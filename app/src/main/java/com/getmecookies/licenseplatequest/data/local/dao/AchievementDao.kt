package com.getmecookies.licenseplatequest.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.getmecookies.licenseplatequest.data.local.entity.AchievementEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AchievementDao {

    /** Persist newly-earned achievements; already-earned ids are ignored (earned-once). */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(achievements: List<AchievementEntity>)

    @Query("SELECT id FROM achievement")
    suspend fun getEarnedIds(): List<String>

    @Query("SELECT id FROM achievement")
    fun observeEarnedIds(): Flow<List<String>>

    /** Earned achievements with their unlock timestamps, for the detail sheet. */
    @Query("SELECT * FROM achievement")
    fun observeEarned(): Flow<List<AchievementEntity>>

    /** Delete every earned achievement (debug-only wipe). */
    @Query("DELETE FROM achievement")
    suspend fun deleteAll()
}
