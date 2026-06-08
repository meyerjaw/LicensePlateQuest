package com.getmecookies.licenseplatequest.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.getmecookies.licenseplatequest.data.local.entity.TripStopEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Dao
interface TripStopDao {

    @Insert
    suspend fun insertAll(stops: List<TripStopEntity>)

    @Query("SELECT * FROM trip_stop WHERE trip_id = :tripId ORDER BY position")
    suspend fun getForTrip(tripId: UUID): List<TripStopEntity>

    @Query("SELECT * FROM trip_stop WHERE trip_id = :tripId ORDER BY position")
    fun observeForTrip(tripId: UUID): Flow<List<TripStopEntity>>

    @Query("DELETE FROM trip_stop WHERE trip_id = :tripId")
    suspend fun deleteForTrip(tripId: UUID)
}
