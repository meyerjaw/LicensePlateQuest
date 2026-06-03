package com.getmecookies.licenseplatequest.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Junction linking a [SpottingEntity] to the players credited for it (playtest note #17 —
 * multi-select attribution). A spotting can be credited to several players; the unique index on
 * (spotting_id, player_id) prevents duplicate credits. Mirrors the [TripPlayerEntity] pattern:
 * a surrogate [id] PK plus FKs. Deleting a spotting cascades; players are soft-deleted (their
 * rows persist), so a credit stays valid even after a player is removed from the roster.
 */
@Entity(
    tableName = "spotting_player",
    foreignKeys = [
        ForeignKey(
            entity = SpottingEntity::class,
            parentColumns = ["id"],
            childColumns = ["spotting_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = PlayerEntity::class,
            parentColumns = ["id"],
            childColumns = ["player_id"],
        ),
    ],
    indices = [
        Index("spotting_id"),
        Index("player_id"),
        Index(value = ["spotting_id", "player_id"], unique = true),
    ],
)
data class SpottingPlayerEntity(
    @PrimaryKey val id: UUID,
    @ColumnInfo(name = "spotting_id") val spottingId: UUID,
    @ColumnInfo(name = "player_id") val playerId: UUID,
)
