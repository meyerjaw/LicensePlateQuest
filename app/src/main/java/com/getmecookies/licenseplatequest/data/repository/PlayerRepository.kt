package com.getmecookies.licenseplatequest.data.repository

import com.getmecookies.licenseplatequest.data.local.dao.EventLogDao
import com.getmecookies.licenseplatequest.data.local.dao.PlayerDao
import com.getmecookies.licenseplatequest.data.local.dao.PlayerWithStats
import com.getmecookies.licenseplatequest.data.local.dao.TripPlayerDao
import com.getmecookies.licenseplatequest.data.local.entity.EventLogEntity
import com.getmecookies.licenseplatequest.data.local.entity.PlayerEntity
import com.getmecookies.licenseplatequest.domain.model.Player
import com.getmecookies.licenseplatequest.domain.model.PlayerListItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.util.UUID

/**
 * Repository for the player roster (SPEC §6 Players Management, §7 Player table).
 *
 * Deletes are soft (sets [PlayerEntity.deleted]) so trip history that references the player
 * stays intact (SPEC §7 invariant, §10 edge case). Names are stored trimmed; callers are
 * expected to have validated non-blankness, but trimming here is a safety net.
 */
class PlayerRepository(
    private val playerDao: PlayerDao,
    private val tripPlayerDao: TripPlayerDao,
    private val eventLogDao: EventLogDao,
) {
    /** Active (non-deleted) players, alphabetized, as domain models. */
    fun observePlayers(): Flow<List<Player>> =
        playerDao.observeActive().map { list -> list.map { it.toDomain() } }

    /** Active players with trip-based play stats for the roster list rows. */
    fun observePlayersWithStats(): Flow<List<PlayerListItem>> =
        playerDao.observeActiveWithStats().map { rows -> rows.map { it.toListItem() } }

    suspend fun addPlayer(name: String, color: String? = null): UUID {
        val now = Instant.now()
        val id = UUID.randomUUID()
        playerDao.insert(
            PlayerEntity(
                id = id,
                name = name.trim(),
                createdAt = now,
                updatedAt = now,
                deleted = false,
                color = color,
            ),
        )
        logEvent("player_added", id, name.trim())
        return id
    }

    suspend fun renamePlayer(id: UUID, newName: String) {
        val existing = playerDao.getById(id) ?: return
        playerDao.update(
            existing.copy(
                name = newName.trim(),
                updatedAt = Instant.now(),
            ),
        )
    }

    /** Update a player's chosen color token (playtest note #19). */
    suspend fun setPlayerColor(id: UUID, color: String?) {
        val existing = playerDao.getById(id) ?: return
        playerDao.update(existing.copy(color = color, updatedAt = Instant.now()))
    }

    /** Soft-delete: keeps the row (and any TripPlayer history) but hides it from the roster. */
    suspend fun deletePlayer(id: UUID) {
        val existing = playerDao.getById(id) ?: return
        playerDao.update(
            existing.copy(
                deleted = true,
                updatedAt = Instant.now(),
            ),
        )
        logEvent("player_deleted", id, existing.name)
    }

    /** Number of trips this player is on — used to warn before deleting (SPEC §10). */
    suspend fun tripCountForPlayer(id: UUID): Int =
        tripPlayerDao.countTripsForPlayer(id)

    /**
     * True if another active player already has this name (case-insensitive). [excludeId] lets
     * a rename keep its own name. Used to surface a friendly "already exists" validation error.
     */
    suspend fun nameExists(name: String, excludeId: UUID? = null): Boolean =
        playerDao.countActiveByName(name.trim(), excludeId ?: ZERO_UUID) > 0

    private companion object {
        // A UUID that can never collide with a real (random) player id, for "exclude nothing".
        val ZERO_UUID: UUID = UUID(0L, 0L)
    }

    private suspend fun logEvent(type: String, playerId: UUID, name: String) {
        eventLogDao.insert(
            EventLogEntity(
                id = UUID.randomUUID(),
                eventType = type,
                payload = """{"player_id":"$playerId","name":${jsonString(name)}}""",
                timestamp = Instant.now(),
            ),
        )
    }

    /** Minimal JSON string escaping for the event payload. */
    private fun jsonString(value: String): String {
        val escaped = value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
        return "\"$escaped\""
    }

    private fun PlayerEntity.toDomain(): Player = Player(id = id, name = name, color = color)

    private fun PlayerWithStats.toListItem(): PlayerListItem = PlayerListItem(
        player = player.toDomain(),
        tripCount = tripCount,
        lastPlayed = lastPlayed,
    )
}
