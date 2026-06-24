package com.getmecookies.licenseplatequest.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
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

    /**
     * Active players with trip-based play stats. LEFT JOINs so players with no trips still
     * appear (trip_count = 0, last_played = NULL). start_date is stored as an ISO-8601 date
     * string, so MAX() orders it chronologically.
     */
    @Query(
        """
        SELECT p.*,
               COUNT(tp.id) AS trip_count,
               MAX(t.start_date) AS last_played
        FROM player p
        LEFT JOIN trip_player tp ON tp.player_id = p.id
        LEFT JOIN trip t ON t.id = tp.trip_id
        WHERE p.deleted = 0
        GROUP BY p.id
        ORDER BY p.name COLLATE NOCASE
        """
    )
    fun observeActiveWithStats(): Flow<List<PlayerWithStats>>

    @Query("SELECT * FROM player WHERE id = :id")
    suspend fun getById(id: UUID): PlayerEntity?

    /**
     * Count active players whose name matches (case-insensitively), optionally excluding one
     * id (so renaming a player to its own current name isn't flagged as a duplicate).
     */
    @Query(
        "SELECT COUNT(*) FROM player WHERE deleted = 0 " +
            "AND name = :name COLLATE NOCASE AND id != :excludeId"
    )
    suspend fun countActiveByName(name: String, excludeId: UUID): Int

    /** Delete every player (debug-only wipe). */
    @Query("DELETE FROM player")
    suspend fun deleteAll()

    /** All players (backup export). */
    @Query("SELECT * FROM player")
    suspend fun getAll(): List<PlayerEntity>

    /** Bulk insert, skipping rows whose primary key already exists (backup import / merge). */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAllIgnore(players: List<PlayerEntity>)
}
