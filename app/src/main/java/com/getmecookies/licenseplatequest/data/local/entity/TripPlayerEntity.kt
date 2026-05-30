package com.getmecookies.licenseplatequest.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant
import java.util.UUID

/**
 * Junction of players on a trip (SPEC §7). [joinedAt] supports adding a player mid-trip.
 * Rows are kept even if the player is soft-deleted, preserving trip history.
 */
@Entity(
    tableName = "trip_player",
    foreignKeys = [
        ForeignKey(
            entity = TripEntity::class,
            parentColumns = ["id"],
            childColumns = ["trip_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = PlayerEntity::class,
            parentColumns = ["id"],
            childColumns = ["player_id"],
        ),
    ],
    indices = [
        Index("trip_id"),
        Index("player_id"),
        Index(value = ["trip_id", "player_id"], unique = true),
    ],
)
data class TripPlayerEntity(
    @PrimaryKey val id: UUID,
    @ColumnInfo(name = "trip_id") val tripId: UUID,
    @ColumnInfo(name = "player_id") val playerId: UUID,
    @ColumnInfo(name = "joined_at") val joinedAt: Instant,
)
