package com.getmecookies.licenseplatequest.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant
import java.util.UUID

/**
 * Append-only log of meaningful actions (SPEC §7): `state_found`, `trip_started`,
 * `trip_ended`, `player_added`, etc. Lets future achievements/badges be computed
 * retroactively over history. [payload] is a raw JSON string.
 */
@Entity(tableName = "event_log")
data class EventLogEntity(
    @PrimaryKey val id: UUID,
    @ColumnInfo(name = "event_type") val eventType: String,
    val payload: String,
    val timestamp: Instant,
)
