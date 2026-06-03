package com.getmecookies.licenseplatequest.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.getmecookies.licenseplatequest.data.local.entity.SpottingPlayerEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID

/** Per-player credit count within a game, for the summary leaderboard (playtest note #18). */
data class PlayerCreditRow(
    val player_id: UUID,
    val count: Int,
)

@Dao
interface SpottingPlayerDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(link: SpottingPlayerEntity)

    /** Replace all credits for a spotting: clear then re-add (used when (re)attributing). */
    @Query("DELETE FROM spotting_player WHERE spotting_id = :spottingId")
    suspend fun deleteForSpotting(spottingId: UUID)

    @Query("SELECT player_id FROM spotting_player WHERE spotting_id = :spottingId")
    suspend fun getPlayerIdsForSpotting(spottingId: UUID): List<UUID>

    @Query("SELECT player_id FROM spotting_player WHERE spotting_id = :spottingId")
    fun observePlayerIdsForSpotting(spottingId: UUID): Flow<List<UUID>>

    /**
     * Per-player credit counts across a whole game instance, newest games included. Unattributed
     * spottings simply don't appear here; the leaderboard derives "unattributed" separately.
     */
    @Query(
        """
        SELECT sp.player_id AS player_id, COUNT(*) AS count
        FROM spotting_player sp
        JOIN spotting s ON s.id = sp.spotting_id
        WHERE s.game_instance_id = :gameInstanceId
        GROUP BY sp.player_id
        """
    )
    suspend fun creditCountsForGame(gameInstanceId: UUID): List<PlayerCreditRow>
}
