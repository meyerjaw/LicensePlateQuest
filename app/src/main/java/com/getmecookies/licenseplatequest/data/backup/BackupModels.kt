package com.getmecookies.licenseplatequest.data.backup

import com.getmecookies.licenseplatequest.data.local.entity.AchievementEntity
import com.getmecookies.licenseplatequest.data.local.entity.EventLogEntity
import com.getmecookies.licenseplatequest.data.local.entity.GameInstanceEntity
import com.getmecookies.licenseplatequest.data.local.entity.PlayerEntity
import com.getmecookies.licenseplatequest.data.local.entity.SpottingEntity
import com.getmecookies.licenseplatequest.data.local.entity.SpottingPlayerEntity
import com.getmecookies.licenseplatequest.data.local.entity.TripEntity
import com.getmecookies.licenseplatequest.data.local.entity.TripPlayerEntity
import com.getmecookies.licenseplatequest.data.local.entity.TripStopEntity
import com.getmecookies.licenseplatequest.domain.model.TripStatus
import kotlinx.serialization.Serializable
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

/**
 * On-disk backup format (export/import — "basic local backup", separate from any future online
 * sync). The file is plain JSON: UUIDs/dates are stored as strings so the format is human-readable
 * and decoupled from the Room entities (which can change behind migrations).
 *
 * Reference data (`plate_region`, `game_type`) is deliberately **not** included: those rows are
 * reseeded from the bundled assets with deterministic ids ([com.getmecookies.licenseplatequest.data.seed.Ids]),
 * so foreign keys (region/game-type) still resolve after a restore on any device.
 */
const val BACKUP_FORMAT_VERSION = 1

@Serializable
data class BackupFile(
    /** Backup-file schema version (this file's shape), independent of [appDbVersion]. */
    val format: Int = BACKUP_FORMAT_VERSION,
    /** The Room DB version at export time, so import can refuse a file from a newer app. */
    val appDbVersion: Int,
    /** ISO-8601 instant the backup was created. */
    val exportedAt: String,
    val data: BackupData,
)

@Serializable
data class BackupData(
    val players: List<PlayerBackup> = emptyList(),
    val trips: List<TripBackup> = emptyList(),
    val tripPlayers: List<TripPlayerBackup> = emptyList(),
    val tripStops: List<TripStopBackup> = emptyList(),
    val gameInstances: List<GameInstanceBackup> = emptyList(),
    val spottings: List<SpottingBackup> = emptyList(),
    val spottingPlayers: List<SpottingPlayerBackup> = emptyList(),
    val achievements: List<AchievementBackup> = emptyList(),
    val eventLog: List<EventLogBackup> = emptyList(),
    val settings: SettingsBackup = SettingsBackup(),
)

@Serializable
data class PlayerBackup(
    val id: String,
    val name: String,
    val createdAt: String,
    val updatedAt: String,
    val deleted: Boolean = false,
    val color: String? = null,
)

@Serializable
data class TripBackup(
    val id: String,
    val name: String,
    val originCity: String,
    val originRegionId: String,
    val destinationCity: String,
    val destinationRegionId: String,
    val startDate: String,
    val endDate: String? = null,
    val status: String,
    val endedAt: String? = null,
    val createdAt: String,
    val updatedAt: String,
)

@Serializable
data class TripPlayerBackup(
    val id: String,
    val tripId: String,
    val playerId: String,
    val joinedAt: String,
)

@Serializable
data class TripStopBackup(
    val id: String,
    val tripId: String,
    val position: Int,
    val regionId: String,
    val city: String,
)

@Serializable
data class GameInstanceBackup(
    val id: String,
    val tripId: String,
    val gameTypeId: String,
    val createdAt: String,
)

@Serializable
data class SpottingBackup(
    val id: String,
    val gameInstanceId: String,
    val plateRegionId: String,
    val spotterPlayerId: String? = null,
    val timestamp: String,
    val note: String? = null,
    val photoPath: String? = null,
    val gpsLat: Double? = null,
    val gpsLng: Double? = null,
    val createdAt: String,
    val celebratedAt: String? = null,
)

@Serializable
data class SpottingPlayerBackup(
    val id: String,
    val spottingId: String,
    val playerId: String,
)

@Serializable
data class AchievementBackup(
    val id: String,
    val earnedAt: String,
)

@Serializable
data class EventLogBackup(
    val id: String,
    val eventType: String,
    val payload: String,
    val timestamp: String,
)

/** User settings (SharedPreferences). Nulls mean "not present in this backup" → leave as-is. */
@Serializable
data class SettingsBackup(
    val themeMode: String? = null,
    val hapticsEnabled: Boolean? = null,
    val soundEnabled: Boolean? = null,
    val tripRemindersEnabled: Boolean? = null,
    val analyticsEnabled: Boolean? = null,
    val homeRegionId: String? = null,
    val homeCity: String? = null,
)

// ---- Entity ↔ backup mapping ---------------------------------------------------------------------

fun PlayerEntity.toBackup() = PlayerBackup(
    id.toString(), name, createdAt.toString(), updatedAt.toString(), deleted, color,
)

fun PlayerBackup.toEntity() = PlayerEntity(
    id = UUID.fromString(id),
    name = name,
    createdAt = Instant.parse(createdAt),
    updatedAt = Instant.parse(updatedAt),
    deleted = deleted,
    color = color,
)

fun TripEntity.toBackup() = TripBackup(
    id = id.toString(),
    name = name,
    originCity = originCity,
    originRegionId = originRegionId.toString(),
    destinationCity = destinationCity,
    destinationRegionId = destinationRegionId.toString(),
    startDate = startDate.toString(),
    endDate = endDate?.toString(),
    status = status.name,
    endedAt = endedAt?.toString(),
    createdAt = createdAt.toString(),
    updatedAt = updatedAt.toString(),
)

fun TripBackup.toEntity() = TripEntity(
    id = UUID.fromString(id),
    name = name,
    originCity = originCity,
    originRegionId = UUID.fromString(originRegionId),
    destinationCity = destinationCity,
    destinationRegionId = UUID.fromString(destinationRegionId),
    startDate = LocalDate.parse(startDate),
    endDate = endDate?.let(LocalDate::parse),
    status = TripStatus.valueOf(status),
    endedAt = endedAt?.let(Instant::parse),
    createdAt = Instant.parse(createdAt),
    updatedAt = Instant.parse(updatedAt),
)

fun TripPlayerEntity.toBackup() =
    TripPlayerBackup(id.toString(), tripId.toString(), playerId.toString(), joinedAt.toString())

fun TripPlayerBackup.toEntity() = TripPlayerEntity(
    id = UUID.fromString(id),
    tripId = UUID.fromString(tripId),
    playerId = UUID.fromString(playerId),
    joinedAt = Instant.parse(joinedAt),
)

fun TripStopEntity.toBackup() =
    TripStopBackup(id.toString(), tripId.toString(), position, regionId.toString(), city)

fun TripStopBackup.toEntity() = TripStopEntity(
    id = UUID.fromString(id),
    tripId = UUID.fromString(tripId),
    position = position,
    regionId = UUID.fromString(regionId),
    city = city,
)

fun GameInstanceEntity.toBackup() =
    GameInstanceBackup(id.toString(), tripId.toString(), gameTypeId.toString(), createdAt.toString())

fun GameInstanceBackup.toEntity() = GameInstanceEntity(
    id = UUID.fromString(id),
    tripId = UUID.fromString(tripId),
    gameTypeId = UUID.fromString(gameTypeId),
    createdAt = Instant.parse(createdAt),
)

fun SpottingEntity.toBackup() = SpottingBackup(
    id = id.toString(),
    gameInstanceId = gameInstanceId.toString(),
    plateRegionId = plateRegionId.toString(),
    spotterPlayerId = spotterPlayerId?.toString(),
    timestamp = timestamp.toString(),
    note = note,
    photoPath = photoPath,
    gpsLat = gpsLat,
    gpsLng = gpsLng,
    createdAt = createdAt.toString(),
    celebratedAt = celebratedAt?.toString(),
)

fun SpottingBackup.toEntity() = SpottingEntity(
    id = UUID.fromString(id),
    gameInstanceId = UUID.fromString(gameInstanceId),
    plateRegionId = UUID.fromString(plateRegionId),
    spotterPlayerId = spotterPlayerId?.let(UUID::fromString),
    timestamp = Instant.parse(timestamp),
    note = note,
    photoPath = photoPath,
    gpsLat = gpsLat,
    gpsLng = gpsLng,
    createdAt = Instant.parse(createdAt),
    celebratedAt = celebratedAt?.let(Instant::parse),
)

fun SpottingPlayerEntity.toBackup() =
    SpottingPlayerBackup(id.toString(), spottingId.toString(), playerId.toString())

fun SpottingPlayerBackup.toEntity() = SpottingPlayerEntity(
    id = UUID.fromString(id),
    spottingId = UUID.fromString(spottingId),
    playerId = UUID.fromString(playerId),
)

fun AchievementEntity.toBackup() = AchievementBackup(id, earnedAt.toString())

fun AchievementBackup.toEntity() = AchievementEntity(id = id, earnedAt = Instant.parse(earnedAt))

fun EventLogEntity.toBackup() =
    EventLogBackup(id.toString(), eventType, payload, timestamp.toString())

fun EventLogBackup.toEntity() = EventLogEntity(
    id = UUID.fromString(id),
    eventType = eventType,
    payload = payload,
    timestamp = Instant.parse(timestamp),
)
