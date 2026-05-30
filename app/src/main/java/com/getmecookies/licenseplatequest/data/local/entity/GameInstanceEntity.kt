package com.getmecookies.licenseplatequest.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant
import java.util.UUID

/**
 * One running game within a trip (SPEC §7). In MVP every new trip auto-creates a single
 * GameInstance of type `license_plate`; the user never sees this indirection.
 */
@Entity(
    tableName = "game_instance",
    foreignKeys = [
        ForeignKey(
            entity = TripEntity::class,
            parentColumns = ["id"],
            childColumns = ["trip_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = GameTypeEntity::class,
            parentColumns = ["id"],
            childColumns = ["game_type_id"],
        ),
    ],
    indices = [
        Index("trip_id"),
        Index("game_type_id"),
    ],
)
data class GameInstanceEntity(
    @PrimaryKey val id: UUID,
    @ColumnInfo(name = "trip_id") val tripId: UUID,
    @ColumnInfo(name = "game_type_id") val gameTypeId: UUID,
    @ColumnInfo(name = "created_at") val createdAt: Instant,
)
