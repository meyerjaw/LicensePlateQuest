package com.getmecookies.licenseplatequest.data.repository

import androidx.room.withTransaction
import com.getmecookies.licenseplatequest.data.local.AppDatabase
import com.getmecookies.licenseplatequest.data.local.dao.TripListRow
import com.getmecookies.licenseplatequest.data.local.entity.EventLogEntity
import com.getmecookies.licenseplatequest.data.local.entity.GameInstanceEntity
import com.getmecookies.licenseplatequest.data.local.entity.TripEntity
import com.getmecookies.licenseplatequest.data.local.entity.TripPlayerEntity
import com.getmecookies.licenseplatequest.data.local.entity.TripStopEntity
import com.getmecookies.licenseplatequest.data.seed.RegionSeeder
import com.getmecookies.licenseplatequest.domain.model.TripListItem
import com.getmecookies.licenseplatequest.domain.model.TripStatus
import com.getmecookies.licenseplatequest.domain.model.TripStop
import com.getmecookies.licenseplatequest.notifications.ReminderScheduler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

/**
 * Repository for trips (SPEC sections 6/7). [createTrip] enforces the core invariant — at
 * most one ACTIVE trip — by demoting the current active trip to IN_PROGRESS inside the same
 * transaction that inserts the new one. It also auto-creates the single license_plate
 * GameInstance (SPEC section 7) so future multi-game support needs no trip rework, links the
 * selected players, and writes a trip_started EventLog row.
 */
class TripRepository(
    private val database: AppDatabase,
    private val reminderScheduler: ReminderScheduler,
) {
    private val tripDao = database.tripDao()
    private val tripPlayerDao = database.tripPlayerDao()
    private val tripStopDao = database.tripStopDao()
    private val gameInstanceDao = database.gameInstanceDao()
    private val gameTypeDao = database.gameTypeDao()
    private val eventLogDao = database.eventLogDao()

    /** Count of all trips — used for lightweight verification before the full list lands. */
    fun observeTripCount(): Flow<Int> = tripDao.observeAll().map { it.size }

    /** All trips with their found-state counts, as domain models for the Trip List. */
    fun observeTripListItems(): Flow<List<TripListItem>> =
        tripDao.observeTripListRows().map { rows -> rows.map { it.toListItem() } }

    /** The current active trip (or null), observed so the Active Trip View reacts to changes. */
    fun observeActiveTrip(): Flow<TripEntity?> = tripDao.observeByStatus(TripStatus.ACTIVE)

    /** A single trip by id, or null. Used by the reminder worker to re-check state at fire time. */
    suspend fun getTrip(tripId: UUID): TripEntity? = tripDao.getById(tripId)

    /** The trip's ordered stops (playtest #11): first = start, last = destination. */
    fun observeStopsForTrip(tripId: UUID): Flow<List<TripStop>> =
        tripStopDao.observeForTrip(tripId).map { rows -> rows.map { TripStop(it.regionId, it.city) } }

    /** One-shot ordered stops for [tripId]. */
    suspend fun getStops(tripId: UUID): List<TripStop> =
        tripStopDao.getForTrip(tripId).map { TripStop(it.regionId, it.city) }

    /** Ordered region codes for a trip's stops (for drawing the map route — playtest #11). */
    fun observeStopCodesForTrip(tripId: UUID): Flow<List<String>> =
        tripStopDao.observeStopCodesForTrip(tripId)

    /** Player ids on a trip (join order), observed for the manage-players screen. */
    fun observePlayerIdsForTrip(tripId: UUID): Flow<List<UUID>> =
        tripPlayerDao.observePlayerIdsForTrip(tripId)

    /** Add an existing player to a trip. No-ops if they're already on it. */
    suspend fun addPlayerToTrip(tripId: UUID, playerId: UUID) {
        if (tripPlayerDao.isOnTrip(tripId, playerId) > 0) return
        tripPlayerDao.insert(
            TripPlayerEntity(
                id = UUID.randomUUID(),
                tripId = tripId,
                playerId = playerId,
                joinedAt = Instant.now(),
            ),
        )
    }

    /** Remove a player from a trip (keeps the player in the roster). */
    suspend fun removePlayerFromTrip(tripId: UUID, playerId: UUID) {
        tripPlayerDao.removeFromTrip(tripId, playerId)
    }

    /**
     * Manually end a trip (SPEC: a trip becomes COMPLETED only on manual end, not on 50/50).
     * Records [TripEntity.endedAt] and logs a trip_ended event.
     */
    suspend fun endTrip(tripId: UUID) {
        val trip = tripDao.getById(tripId) ?: return
        // Idempotent: a trip is finalized as soon as the user confirms the end, so re-finalizing
        // from the celebration's "Done" keeps the original end time and logs no duplicate.
        if (trip.status == TripStatus.COMPLETED) return
        val now = Instant.now()
        tripDao.update(trip.copy(status = TripStatus.COMPLETED, endedAt = now, updatedAt = now))
        eventLogDao.insert(
            EventLogEntity(
                id = UUID.randomUUID(),
                eventType = "trip_ended",
                payload = """{"trip_id":"$tripId"}""",
                timestamp = now,
            ),
        )
        // The trip is wrapped up — drop any pending overdue reminder (playtest #13).
        reminderScheduler.cancelForTrip(tripId)
    }

    /**
     * Update an existing trip's editable fields — name, origin/destination, and start/end dates
     * (playtest #14, Manage trip). Players are synced separately via [addPlayerToTrip] /
     * [removePlayerFromTrip]. When the end date changes, the overdue reminder (playtest #13) is
     * re-armed for a remaining future end date on a non-completed trip, or cancelled otherwise.
     */
    suspend fun updateTrip(
        tripId: UUID,
        name: String,
        stops: List<TripStop>,
        startDate: LocalDate,
        endDate: LocalDate?,
    ) {
        val existing = tripDao.getById(tripId) ?: return
        val origin = stops.first()
        val destination = stops.last()
        val now = Instant.now()
        tripDao.update(
            existing.copy(
                name = name.trim(),
                originCity = origin.city.trim(),
                originRegionId = origin.regionId,
                destinationCity = destination.city.trim(),
                destinationRegionId = destination.regionId,
                startDate = startDate,
                endDate = endDate,
                updatedAt = now,
            ),
        )
        // Rewrite the route stops (playtest #11); legacy origin/destination columns stay in sync.
        tripStopDao.deleteForTrip(tripId)
        tripStopDao.insertAll(stopEntities(tripId, stops))
        // Only touch the reminder when the end date actually moved (keyed-on-date dedup in the
        // scheduler then allows a fresh nudge for the new date).
        if (existing.endDate != endDate) {
            if (endDate != null && existing.status != TripStatus.COMPLETED) {
                reminderScheduler.scheduleForTrip(tripId, endDate)
            } else {
                reminderScheduler.cancelForTrip(tripId)
            }
        }
    }

    /** Legacy 2-stop update; delegates to the stops-based [updateTrip]. */
    suspend fun updateTrip(
        tripId: UUID,
        name: String,
        originCity: String,
        originRegionId: UUID,
        destinationCity: String,
        destinationRegionId: UUID,
        startDate: LocalDate,
        endDate: LocalDate?,
    ) = updateTrip(
        tripId = tripId,
        name = name,
        stops = listOf(
            TripStop(originRegionId, originCity),
            TripStop(destinationRegionId, destinationCity),
        ),
        startDate = startDate,
        endDate = endDate,
    )

    /**
     * Make [tripId] the active trip (SPEC: selecting a trip activates it). Demotes any other
     * active trip to IN_PROGRESS in the same transaction. A COMPLETED trip stays completed.
     */
    suspend fun setActiveTrip(tripId: UUID) = database.withTransaction {
        val target = tripDao.getById(tripId) ?: return@withTransaction
        if (target.status == TripStatus.ACTIVE) return@withTransaction
        val now = Instant.now()
        tripDao.getByStatus(TripStatus.ACTIVE)?.let { current ->
            if (current.id != tripId) {
                tripDao.update(current.copy(status = TripStatus.IN_PROGRESS, updatedAt = now))
            }
        }
        // Don't resurrect a completed trip into active; only in-progress trips re-activate.
        if (target.status == TripStatus.IN_PROGRESS) {
            tripDao.update(target.copy(status = TripStatus.ACTIVE, updatedAt = now))
        }
    }

    /** Delete a trip and (via FK cascade) its players links, game instances, and spottings. */
    suspend fun deleteTrip(tripId: UUID) {
        val trip = tripDao.getById(tripId) ?: return
        tripDao.delete(trip)
        eventLogDao.insert(
            EventLogEntity(
                id = UUID.randomUUID(),
                eventType = "trip_deleted",
                payload = """{"trip_id":"$tripId"}""",
                timestamp = Instant.now(),
            ),
        )
        // No trip, no reminder (playtest #13).
        reminderScheduler.cancelForTrip(tripId)
    }

    /** Build the ordered route-stop rows for a trip (position = list index). */
    private fun stopEntities(tripId: UUID, stops: List<TripStop>): List<TripStopEntity> =
        stops.mapIndexed { index, stop ->
            TripStopEntity(UUID.randomUUID(), tripId, index, stop.regionId, stop.city.trim())
        }

    private fun TripListRow.toListItem(): TripListItem = TripListItem(
        id = trip.id,
        name = trip.name,
        status = trip.status,
        startDate = trip.startDate,
        endDate = trip.endDate,
        endedAt = trip.endedAt,
        createdAt = trip.createdAt,
        foundCount = foundCount,
    )

    /**
     * Create a trip from an ordered list of [stops] (first = start, last = destination; playtest
     * #11), make it the active one, attach its players, and start its license-plate game. Runs in
     * a transaction so the "one active trip" rule can never be left half-applied. The legacy
     * origin/destination columns are kept in sync with the first/last stops.
     *
     * @return the new trip's id.
     */
    suspend fun createTrip(
        name: String,
        stops: List<TripStop>,
        startDate: LocalDate,
        endDate: LocalDate? = null,
        playerIds: List<UUID>,
    ): UUID {
        val origin = stops.first()
        val destination = stops.last()
        val createdTripId = database.withTransaction {
        val now = Instant.now()

        // Demote any currently-active trip (SPEC section 7 invariant).
        tripDao.getByStatus(TripStatus.ACTIVE)?.let { current ->
            tripDao.update(current.copy(status = TripStatus.IN_PROGRESS, updatedAt = now))
        }

        val tripId = UUID.randomUUID()
        tripDao.insert(
            TripEntity(
                id = tripId,
                name = name.trim(),
                originCity = origin.city.trim(),
                originRegionId = origin.regionId,
                destinationCity = destination.city.trim(),
                destinationRegionId = destination.regionId,
                startDate = startDate,
                endDate = endDate,
                status = TripStatus.ACTIVE,
                endedAt = null,
                createdAt = now,
                updatedAt = now,
            ),
        )

        playerIds.distinct().forEach { playerId ->
            tripPlayerDao.insert(
                TripPlayerEntity(
                    id = UUID.randomUUID(),
                    tripId = tripId,
                    playerId = playerId,
                    joinedAt = now,
                ),
            )
        }

        // Every trip gets one license-plate game instance (invisible to the user in MVP).
        gameTypeDao.getByCode(RegionSeeder.LICENSE_PLATE_CODE)?.let { gameType ->
            gameInstanceDao.insert(
                GameInstanceEntity(
                    id = UUID.randomUUID(),
                    tripId = tripId,
                    gameTypeId = gameType.id,
                    createdAt = now,
                ),
            )
        }

        eventLogDao.insert(
            EventLogEntity(
                id = UUID.randomUUID(),
                eventType = "trip_started",
                payload = """{"trip_id":"$tripId","player_count":${playerIds.distinct().size}}""",
                timestamp = now,
            ),
        )

        // Seed the route stops (playtest #11).
        tripStopDao.insertAll(stopEntities(tripId, stops))

        tripId
        }
        // Schedule the overdue reminder outside the transaction (playtest #13). Only trips with an
        // end date get one; WorkManager persists it across process death and reboot.
        endDate?.let { reminderScheduler.scheduleForTrip(createdTripId, it) }
        return createdTripId
    }

    /** Legacy 2-stop create (origin → destination); delegates to the stops-based [createTrip]. */
    suspend fun createTrip(
        name: String,
        originCity: String,
        originRegionId: UUID,
        destinationCity: String,
        destinationRegionId: UUID,
        startDate: LocalDate,
        endDate: LocalDate? = null,
        playerIds: List<UUID>,
    ): UUID = createTrip(
        name = name,
        stops = listOf(
            TripStop(originRegionId, originCity),
            TripStop(destinationRegionId, destinationCity),
        ),
        startDate = startDate,
        endDate = endDate,
        playerIds = playerIds,
    )
}
