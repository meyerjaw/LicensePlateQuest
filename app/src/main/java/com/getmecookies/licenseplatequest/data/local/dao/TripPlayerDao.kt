package com.getmecookies.licenseplatequest.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.getmecookies.licenseplatequest.data.local.entity.PlayerEntity
import com.getmecookies.licenseplatequest.data.local.entity.TripPlayerEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Dao
interface TripPlayerDao {

    @Insert
    suspend fun insert(tripPlayer: TripPlayerEntity)

    @Query("SELECT * FROM trip_player WHERE trip_id = :tripId")
    suspend fun getForTrip(tripId: UUID): List<TripPlayerEntity>

    /** Player ids on a trip, in join order — observed so the manage-players screen reacts live. */
    @Query("SELECT player_id FROM trip_player WHERE trip_id = :tripId ORDER BY joined_at")
    fun observePlayerIdsForTrip(tripId: UUID): Flow<List<UUID>>

    /** Whether a player is already linked to a trip (the link is unique per trip + player). */
    @Query("SELECT COUNT(*) FROM trip_player WHERE trip_id = :tripId AND player_id = :playerId")
    suspend fun isOnTrip(tripId: UUID, playerId: UUID): Int

    /** Remove a player from a trip (does not delete the player themselves). */
    @Query("DELETE FROM trip_player WHERE trip_id = :tripId AND player_id = :playerId")
    suspend fun removeFromTrip(tripId: UUID, playerId: UUID)

    /** How many trips a player belongs to — drives the delete-with-warning flow (SPEC §6/§10). */
    @Query("SELECT COUNT(*) FROM trip_player WHERE player_id = :playerId")
    suspend fun countTripsForPlayer(playerId: UUID): Int

    /** Active players on a trip (with details incl. color), in join order — for attribution. */
    @Query(
        """
        SELECT p.*
        FROM trip_player tp
        JOIN player p ON p.id = tp.player_id
        WHERE tp.trip_id = :tripId AND p.deleted = 0
        ORDER BY tp.joined_at
        """
    )
    suspend fun getPlayersForTrip(tripId: UUID): List<PlayerEntity>

    /** Names of players on a trip, in join order (for the celebration stats screen). */
    @Query(
        """
        SELECT p.name
        FROM trip_player tp
        JOIN player p ON p.id = tp.player_id
        WHERE tp.trip_id = :tripId
        ORDER BY tp.joined_at
        """
    )
    suspend fun getPlayerNamesForTrip(tripId: UUID): List<String>

    /** All trip-player links (backup export). */
    @Query("SELECT * FROM trip_player")
    suspend fun getAll(): List<TripPlayerEntity>

    /** Bulk insert, skipping rows whose primary key already exists (backup import / merge). */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAllIgnore(links: List<TripPlayerEntity>)
}
