package com.getmecookies.licenseplatequest.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
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

    /** Ordered region codes for a trip's stops (start → destination) — for the map route. */
    @Query(
        "SELECT pr.region_code FROM trip_stop ts " +
            "JOIN plate_region pr ON pr.id = ts.region_id " +
            "WHERE ts.trip_id = :tripId ORDER BY ts.position",
    )
    fun observeStopCodesForTrip(tripId: UUID): Flow<List<String>>

    /** Ordered stop places (region code + typed city) — for pinning real cities on the route. */
    @Query(
        "SELECT pr.region_code AS code, ts.city AS city FROM trip_stop ts " +
                "JOIN plate_region pr ON pr.id = ts.region_id " +
                "WHERE ts.trip_id = :tripId ORDER BY ts.position",
    )
    fun observeStopPlacesForTrip(tripId: UUID): Flow<List<StopPlaceRow>>

    @Query("DELETE FROM trip_stop WHERE trip_id = :tripId")
    suspend fun deleteForTrip(tripId: UUID)

    /** All stops (backup export). */
    @Query("SELECT * FROM trip_stop")
    suspend fun getAll(): List<TripStopEntity>

    /** Bulk insert, skipping rows whose primary key already exists (backup import / merge). */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAllIgnore(stops: List<TripStopEntity>)
}

/** A trip stop's region code + the city the user typed (for geocoding the route pin). */
data class StopPlaceRow(val code: String, val city: String)
