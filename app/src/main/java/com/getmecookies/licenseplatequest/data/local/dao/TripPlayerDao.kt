package com.getmecookies.licenseplatequest.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.getmecookies.licenseplatequest.data.local.entity.TripPlayerEntity
import java.util.UUID

@Dao
interface TripPlayerDao {

    @Insert
    suspend fun insert(tripPlayer: TripPlayerEntity)

    @Query("SELECT * FROM trip_player WHERE trip_id = :tripId")
    suspend fun getForTrip(tripId: UUID): List<TripPlayerEntity>

    /** How many trips a player belongs to — drives the delete-with-warning flow (SPEC §6/§10). */
    @Query("SELECT COUNT(*) FROM trip_player WHERE player_id = :playerId")
    suspend fun countTripsForPlayer(playerId: UUID): Int
}
