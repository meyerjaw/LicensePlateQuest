package com.getmecookies.licenseplatequest.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant
import java.util.UUID

/**
 * A single "we saw this state's plate" event within a game (SPEC §7). Several fields are
 * reserved nullable for future phases: [spotterPlayerId] (per-player attribution),
 * [photoPath], [gpsLat]/[gpsLng]. They stay null in MVP.
 */
@Entity(
    tableName = "spotting",
    foreignKeys = [
        ForeignKey(
            entity = GameInstanceEntity::class,
            parentColumns = ["id"],
            childColumns = ["game_instance_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = PlateRegionEntity::class,
            parentColumns = ["id"],
            childColumns = ["plate_region_id"],
        ),
        ForeignKey(
            entity = PlayerEntity::class,
            parentColumns = ["id"],
            childColumns = ["spotter_player_id"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [
        Index("game_instance_id"),
        Index("plate_region_id"),
        Index("spotter_player_id"),
    ],
)
data class SpottingEntity(
    @PrimaryKey val id: UUID,
    @ColumnInfo(name = "game_instance_id") val gameInstanceId: UUID,
    @ColumnInfo(name = "plate_region_id") val plateRegionId: UUID,
    @ColumnInfo(name = "spotter_player_id") val spotterPlayerId: UUID?,
    val timestamp: Instant,
    val note: String?,
    @ColumnInfo(name = "photo_path") val photoPath: String?,
    @ColumnInfo(name = "gps_lat") val gpsLat: Double?,
    @ColumnInfo(name = "gps_lng") val gpsLng: Double?,
    @ColumnInfo(name = "created_at") val createdAt: Instant,
)
