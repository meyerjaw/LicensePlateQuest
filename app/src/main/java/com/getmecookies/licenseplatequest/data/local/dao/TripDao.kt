package com.getmecookies.licenseplatequest.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.getmecookies.licenseplatequest.data.local.entity.TripEntity
import com.getmecookies.licenseplatequest.domain.model.TripStatus
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Dao
interface TripDao {

    @Insert
    suspend fun insert(trip: TripEntity)

    @Update
    suspend fun update(trip: TripEntity)

    @Query("SELECT * FROM trip ORDER BY updated_at DESC")
    fun observeAll(): Flow<List<TripEntity>>

    /**
     * All trips, each with the count of distinct states found in it (across its game
     * instances). LEFT JOINs so trips with no spottings report found_count = 0. Ordered
     * newest-activity first; the ViewModel groups them into status sections.
     */
    @Query(
        """
        SELECT t.*, COUNT(DISTINCT s.plate_region_id) AS found_count
        FROM trip t
        LEFT JOIN game_instance gi ON gi.trip_id = t.id
        LEFT JOIN spotting s ON s.game_instance_id = gi.id
        GROUP BY t.id
        ORDER BY t.updated_at DESC
        """
    )
    fun observeTripListRows(): Flow<List<TripListRow>>

    /** The single active trip, if any (SPEC invariant: at most one). */
    @Query("SELECT * FROM trip WHERE status = :status LIMIT 1")
    suspend fun getByStatus(status: TripStatus): TripEntity?

    /** Observe the single trip in a given status (used to react to the active trip changing). */
    @Query("SELECT * FROM trip WHERE status = :status LIMIT 1")
    fun observeByStatus(status: TripStatus): Flow<TripEntity?>

    @Query("SELECT * FROM trip WHERE id = :id")
    suspend fun getById(id: UUID): TripEntity?

    /** Each trip's status + player count, for achievement stats (completed-trip + team feats). */
    @Query(
        """
        SELECT t.status AS status,
               (SELECT COUNT(*) FROM trip_player tp WHERE tp.trip_id = t.id) AS player_count
        FROM trip t
        """
    )
    suspend fun getStatusPlayerCounts(): List<TripStatusPlayerRow>

    @Delete
    suspend fun delete(trip: TripEntity)
}

/** Projection for a trip's status and how many players are on it. */
data class TripStatusPlayerRow(
    val status: TripStatus,
    @androidx.room.ColumnInfo(name = "player_count") val playerCount: Int,
)
