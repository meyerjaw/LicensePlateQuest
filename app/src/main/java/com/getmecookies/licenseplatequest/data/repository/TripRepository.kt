package com.getmecookies.licenseplatequest.data.repository

import androidx.room.withTransaction
import com.getmecookies.licenseplatequest.data.local.AppDatabase
import com.getmecookies.licenseplatequest.data.local.entity.EventLogEntity
import com.getmecookies.licenseplatequest.data.local.entity.GameInstanceEntity
import com.getmecookies.licenseplatequest.data.local.entity.TripEntity
import com.getmecookies.licenseplatequest.data.local.entity.TripPlayerEntity
import com.getmecookies.licenseplatequest.data.seed.RegionSeeder
import com.getmecookies.licenseplatequest.domain.model.TripStatus
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
) {
    private val tripDao = database.tripDao()
    private val tripPlayerDao = database.tripPlayerDao()
    private val gameInstanceDao = database.gameInstanceDao()
    private val gameTypeDao = database.gameTypeDao()
    private val eventLogDao = database.eventLogDao()

    /** Count of all trips — used for lightweight verification before the full list lands. */
    fun observeTripCount(): Flow<Int> = tripDao.observeAll().map { it.size }

    /**
     * Create a trip, make it the active one, attach its players, and start its license-plate
     * game. Runs in a transaction so the "one active trip" rule can never be left half-applied.
     *
     * @return the new trip's id.
     */
    suspend fun createTrip(
        name: String,
        originCity: String,
        originRegionId: UUID,
        destinationCity: String,
        destinationRegionId: UUID,
        startDate: LocalDate,
        playerIds: List<UUID>,
    ): UUID = database.withTransaction {
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
                originCity = originCity.trim(),
                originRegionId = originRegionId,
                destinationCity = destinationCity.trim(),
                destinationRegionId = destinationRegionId,
                startDate = startDate,
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

        tripId
    }
}
