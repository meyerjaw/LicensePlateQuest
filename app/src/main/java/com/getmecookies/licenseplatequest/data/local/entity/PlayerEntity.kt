package com.getmecookies.licenseplatequest.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant
import java.util.UUID

/**
 * A reusable player profile (SPEC §7). Soft-deletable: deleting a player sets [deleted]
 * so existing trip history (TripPlayer rows, spotting attribution) is preserved.
 */
@Entity(tableName = "player")
data class PlayerEntity(
    @PrimaryKey val id: UUID,
    val name: String,
    @ColumnInfo(name = "created_at") val createdAt: Instant,
    @ColumnInfo(name = "updated_at") val updatedAt: Instant,
    val deleted: Boolean = false,
)
