package com.getmecookies.licenseplatequest.data.repository

import com.getmecookies.licenseplatequest.data.local.AppDatabase
import com.getmecookies.licenseplatequest.data.local.entity.EventLogEntity
import com.getmecookies.licenseplatequest.data.local.entity.SpottingEntity
import com.getmecookies.licenseplatequest.data.local.entity.SpottingPlayerEntity
import com.getmecookies.licenseplatequest.domain.model.FoundState
import com.getmecookies.licenseplatequest.domain.model.Player
import com.getmecookies.licenseplatequest.domain.model.StateDetailData
import com.getmecookies.licenseplatequest.domain.model.StateInfo
import com.getmecookies.licenseplatequest.domain.model.StateSummary
import com.getmecookies.licenseplatequest.domain.model.TripStatus
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import java.time.Instant
import java.util.UUID

/**
 * Marking license-plate spottings against the active trip (SPEC section 6, State Detail).
 *
 * A spotting belongs to a trip's GameInstance (the single license_plate game per trip). All
 * write operations target the *active* trip; if none is active they no-op, since the UI
 * disables marking in that case.
 */
class SpottingRepository(
    private val database: AppDatabase,
) {
    private val tripDao = database.tripDao()
    private val gameInstanceDao = database.gameInstanceDao()
    private val spottingDao = database.spottingDao()
    private val spottingPlayerDao = database.spottingPlayerDao()
    private val tripPlayerDao = database.tripPlayerDao()
    private val plateRegionDao = database.plateRegionDao()
    private val eventLogDao = database.eventLogDao()

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * The set of found state codes for the active trip, as a Flow that re-subscribes whenever
     * the active trip changes. Emits an empty set when no trip is active.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeFoundCodesForActiveTrip(): Flow<Set<String>> =
        tripDao.observeByStatus(TripStatus.ACTIVE).flatMapLatest { trip ->
            if (trip == null) {
                flowOf(emptySet())
            } else {
                val game = gameInstanceDao.getForTrip(trip.id).firstOrNull()
                if (game == null) {
                    flowOf(emptySet())
                } else {
                    spottingDao.observeFoundCodesForGame(game.id).map { it.toSet() }
                }
            }
        }

    /**
     * Found states (with display details) for the active trip, newest-found first. Like
     * [observeFoundCodesForActiveTrip], it re-subscribes when the active trip changes and
     * emits an empty list when no trip is active.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeFoundStatesForActiveTrip(): Flow<List<FoundState>> =
        tripDao.observeByStatus(TripStatus.ACTIVE).flatMapLatest { trip ->
            if (trip == null) {
                flowOf(emptyList())
            } else {
                val game = gameInstanceDao.getForTrip(trip.id).firstOrNull()
                if (game == null) {
                    flowOf(emptyList())
                } else {
                    spottingDao.observeFoundDetailsForGame(game.id).map { rows ->
                        rows.map {
                            FoundState(
                                code = it.code,
                                name = it.name,
                                foundAt = it.foundAt,
                            )
                        }
                    }
                }
            }
        }

    /**
     * Every state (code + name), ordered for display. Used to list unfound states alongside
     * found ones in the Active Trip bottom sheet.
     */
    fun observeAllStates(): Flow<List<StateSummary>> =
        plateRegionDao.observeAll().map { regions ->
            regions.map { StateSummary(code = it.regionCode, name = it.name) }
        }

    /** Load the static facts + active-trip found status for one state. */
    suspend fun getStateDetail(regionCode: String): StateDetailData? {
        val region = plateRegionDao.getByCode(COUNTRY, regionCode) ?: return null
        val info = StateInfo(
            code = region.regionCode,
            name = region.name,
            bird = region.bird,
            motto = region.motto,
            flower = region.flower,
            funFacts = parseFunFacts(region.funFacts),
        )

        val activeTrip = tripDao.getByStatus(TripStatus.ACTIVE)
            ?: return StateDetailData(info = info, hasActiveTrip = false, found = false)

        val game = gameInstanceDao.getForTrip(activeTrip.id).firstOrNull()
            ?: return StateDetailData(info = info, hasActiveTrip = true, found = false)

        val tripPlayers = tripPlayerDao.getPlayersForTrip(activeTrip.id)
            .map { Player(id = it.id, name = it.name, color = it.color) }

        val spotting = spottingDao.getForRegion(game.id, region.id)
        // Pre-selection: the players already credited when found; for an unfound state, auto-credit
        // a sole player but otherwise start empty (no cross-state memory).
        val initialAttribution: Set<UUID> = when {
            spotting != null -> spottingPlayerDao.getPlayerIdsForSpotting(spotting.id).toSet()
            tripPlayers.size == 1 -> setOf(tripPlayers.first().id)
            else -> emptySet()
        }
        return StateDetailData(
            info = info,
            hasActiveTrip = true,
            found = spotting != null,
            foundAt = spotting?.timestamp,
            foundTripName = if (spotting != null) activeTrip.name else null,
            tripPlayers = tripPlayers,
            initialAttribution = initialAttribution,
        )
    }

    /**
     * Mark a state found on the active trip. Idempotent — if it's already marked, does
     * nothing. No-ops when there's no active trip/game.
     *
     * @return true if a new spotting was created.
     */
    suspend fun markState(regionCode: String, playerIds: List<UUID> = emptyList()): Boolean {
        val region = plateRegionDao.getByCode(COUNTRY, regionCode) ?: return false
        val activeTrip = tripDao.getByStatus(TripStatus.ACTIVE) ?: return false
        val game = gameInstanceDao.getForTrip(activeTrip.id).firstOrNull() ?: return false
        if (spottingDao.getForRegion(game.id, region.id) != null) return false

        val now = Instant.now()
        val spottingId = UUID.randomUUID()
        spottingDao.insert(
            SpottingEntity(
                id = spottingId,
                gameInstanceId = game.id,
                plateRegionId = region.id,
                spotterPlayerId = null, // attribution now lives in spotting_player (multi-select)
                timestamp = now,
                note = null,
                photoPath = null,
                gpsLat = null,
                gpsLng = null,
                createdAt = now,
            ),
        )
        playerIds.distinct().forEach { playerId ->
            spottingPlayerDao.insert(SpottingPlayerEntity(UUID.randomUUID(), spottingId, playerId))
        }
        logEvent("state_found", activeTrip.id, region.regionCode)
        return true
    }

    /**
     * Replace the players credited for an already-found state (playtest note #17 — edit credit).
     * Clears existing credits and re-adds the given ones. No-ops if the state isn't found.
     */
    suspend fun setAttribution(regionCode: String, playerIds: List<UUID>) {
        val region = plateRegionDao.getByCode(COUNTRY, regionCode) ?: return
        val activeTrip = tripDao.getByStatus(TripStatus.ACTIVE) ?: return
        val game = gameInstanceDao.getForTrip(activeTrip.id).firstOrNull() ?: return
        val spotting = spottingDao.getForRegion(game.id, region.id) ?: return
        spottingPlayerDao.deleteForSpotting(spotting.id)
        playerIds.distinct().forEach { playerId ->
            spottingPlayerDao.insert(SpottingPlayerEntity(UUID.randomUUID(), spotting.id, playerId))
        }
    }

    /** Unmark a state on the active trip (SPEC: only unmark is supported, not editing). */
    suspend fun unmarkState(regionCode: String) {
        val region = plateRegionDao.getByCode(COUNTRY, regionCode) ?: return
        val activeTrip = tripDao.getByStatus(TripStatus.ACTIVE) ?: return
        val game = gameInstanceDao.getForTrip(activeTrip.id).firstOrNull() ?: return
        val spotting = spottingDao.getForRegion(game.id, region.id) ?: return
        spottingDao.delete(spotting)
        logEvent("state_unmarked", activeTrip.id, region.regionCode)
    }

    private suspend fun logEvent(type: String, tripId: UUID, regionCode: String) {
        eventLogDao.insert(
            EventLogEntity(
                id = UUID.randomUUID(),
                eventType = type,
                payload = """{"trip_id":"$tripId","region_code":"$regionCode"}""",
                timestamp = Instant.now(),
            ),
        )
    }

    private fun parseFunFacts(raw: String): List<String> =
        try {
            json.decodeFromString(ListSerializer(String.serializer()), raw)
        } catch (e: Exception) {
            emptyList()
        }

    private companion object {
        const val COUNTRY = "US"
    }
}
