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

    suspend fun addPlayer(name: String): UUID {
        val now = Instant.now()
        val id = UUID.randomUUID()
        playerDao.insert(
            PlayerEntity(
                id = id,
                name = name.trim(),
                createdAt = now,
                updatedAt = now,
                deleted = false,
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

    private fun PlayerEntity.toDomain(): Player = Player(id = id, name = name)

    private fun PlayerWithStats.toListItem(): PlayerListItem = PlayerListItem(
        player = player.toDomain(),
        tripCount = tripCount,
        lastPlayed = lastPlayed,
    )
}
