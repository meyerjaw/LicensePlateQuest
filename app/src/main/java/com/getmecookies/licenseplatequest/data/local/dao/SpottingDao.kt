package com.getmecookies.licenseplatequest.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.getmecookies.licenseplatequest.data.local.entity.SpottingEntity
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.util.UUID

@Dao
interface SpottingDao {

    @Insert
    suspend fun insert(spotting: SpottingEntity)

    @Delete
    suspend fun delete(spotting: SpottingEntity)

    @Query("SELECT * FROM spotting WHERE game_instance_id = :gameInstanceId ORDER BY timestamp")
    fun observeForGame(gameInstanceId: UUID): Flow<List<SpottingEntity>>

    /** The 2-letter region codes found in a game, for coloring the map. */
    @Query(
        """
        SELECT pr.region_code
        FROM spotting s
        JOIN plate_region pr ON pr.id = s.plate_region_id
        WHERE s.game_instance_id = :gameInstanceId
        """
    )
    fun observeFoundCodesForGame(gameInstanceId: UUID): Flow<List<String>>

    /**
     * Region codes found in a game whose fill animation hasn't played yet (celebrated_at IS NULL).
     * The map animates these on its next visit, so finds made off the map aren't missed (#20).
     */
    @Query(
        """
        SELECT pr.region_code
        FROM spotting s
        JOIN plate_region pr ON pr.id = s.plate_region_id
        WHERE s.game_instance_id = :gameInstanceId AND s.celebrated_at IS NULL
        """
    )
    fun observeUncelebratedCodesForGame(gameInstanceId: UUID): Flow<List<String>>

    /** Stamp the given regions as celebrated (animation played). No-op for already-stamped rows. */
    @Query(
        """
        UPDATE spotting SET celebrated_at = :now
        WHERE game_instance_id = :gameInstanceId AND celebrated_at IS NULL
          AND plate_region_id IN (SELECT id FROM plate_region WHERE region_code IN (:regionCodes))
        """
    )
    suspend fun markCelebrated(gameInstanceId: UUID, regionCodes: List<String>, now: Instant)

    /** Every spotting across all trips, projected for achievement stats (code + time + trip). */
    @Query(
        """
        SELECT pr.region_code AS region_code,
               s.timestamp AS timestamp,
               gi.trip_id AS trip_id
        FROM spotting s
        JOIN plate_region pr ON pr.id = s.plate_region_id
        JOIN game_instance gi ON gi.id = s.game_instance_id
        """
    )
    suspend fun getAllFinds(): List<SpottingFindRow>

    /** The spotting for a given region in a game, if it's been marked. */
    @Query(
        "SELECT * FROM spotting WHERE game_instance_id = :gameInstanceId " +
            "AND plate_region_id = :plateRegionId LIMIT 1"
    )
    suspend fun getForRegion(gameInstanceId: UUID, plateRegionId: UUID): SpottingEntity?

    /**
     * Found states in a game with the details the Active Trip View's bottom sheet needs,
     * newest-found first. Ordering by the ISO-8601 UTC timestamp string sorts chronologically.
     */
    @Query(
        """
        SELECT pr.region_code AS region_code,
               pr.name AS name,
               s.timestamp AS found_at
        FROM spotting s
        JOIN plate_region pr ON pr.id = s.plate_region_id
        WHERE s.game_instance_id = :gameInstanceId
        ORDER BY s.timestamp DESC
        """
    )
    fun observeFoundDetailsForGame(gameInstanceId: UUID): Flow<List<FoundStateRow>>

    /**
     * The lifetime "Plate Passport" (cross-trip collection): every state ever spotted in any trip,
     * with the date it was *first* caught and the trip that caught it. ISO-8601 UTC timestamps sort
     * chronologically as strings, so the earliest-timestamp row (per region) is the first catch.
     * Ordered by state name for display.
     */
    @Query(
        """
        SELECT pr.region_code AS region_code,
               pr.name AS name,
               s.timestamp AS first_found_at,
               t.id AS first_trip_id,
               t.name AS first_trip_name
        FROM spotting s
        JOIN plate_region pr ON pr.id = s.plate_region_id
        JOIN game_instance gi ON gi.id = s.game_instance_id
        JOIN trip t ON t.id = gi.trip_id
        WHERE s.timestamp = (
            SELECT MIN(s2.timestamp) FROM spotting s2 WHERE s2.plate_region_id = s.plate_region_id
        )
        GROUP BY pr.id
        ORDER BY pr.name
        """
    )
    fun observeLifetimeFound(): Flow<List<LifetimeFoundRow>>

    /**
     * All spottings in a game with the region attributes needed to compute celebration stats
     * (name, geographic center, rarity), ordered chronologically.
     */
    @Query(
        """
        SELECT pr.region_code AS region_code,
               pr.name AS name,
               pr.center_lat AS center_lat,
               pr.center_lng AS center_lng,
               pr.rarity_score AS rarity_score,
               s.timestamp AS timestamp
        FROM spotting s
        JOIN plate_region pr ON pr.id = s.plate_region_id
        WHERE s.game_instance_id = :gameInstanceId
        ORDER BY s.timestamp
        """
    )
    suspend fun getStatRows(gameInstanceId: UUID): List<SpottingStatRow>

    /** Spottings in a game with no credited player — the "Unattributed" leaderboard line (#18). */
    @Query(
        """
        SELECT COUNT(*) FROM spotting s
        WHERE s.game_instance_id = :gameInstanceId
          AND NOT EXISTS (SELECT 1 FROM spotting_player sp WHERE sp.spotting_id = s.id)
        """
    )
    suspend fun countUnattributedForGame(gameInstanceId: UUID): Int

    /**
     * How many *other* trips (excluding [excludeTripId]) have ever spotted this region. Zero means
     * the region is brand-new to the lifetime collection — its at-catch "new for your collection!"
     * flourish (Passport follow-up).
     */
    @Query(
        """
        SELECT COUNT(DISTINCT gi.trip_id) FROM spotting s
        JOIN plate_region pr ON pr.id = s.plate_region_id
        JOIN game_instance gi ON gi.id = s.game_instance_id
        WHERE pr.region_code = :regionCode AND gi.trip_id != :excludeTripId
        """
    )
    suspend fun countOtherTripsWithRegion(regionCode: String, excludeTripId: UUID): Int
}
