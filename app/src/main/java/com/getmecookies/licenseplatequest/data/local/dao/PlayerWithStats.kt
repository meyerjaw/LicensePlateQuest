package com.getmecookies.licenseplatequest.data.local.dao

import androidx.room.ColumnInfo
import androidx.room.Embedded
import com.getmecookies.licenseplatequest.data.local.entity.PlayerEntity
import java.time.LocalDate

/**
 * Query projection: a player plus trip-based play stats (SPEC-adjacent, added per product
 * request). [tripCount] is how many trips the player has joined; [lastPlayed] is the most
 * recent joined trip's start date (null if they've never been on a trip).
 *
 * Note: per-plate attribution isn't in the MVP data model, so "plays" is trip-based for now.
 */
data class PlayerWithStats(
    @Embedded val player: PlayerEntity,
    @ColumnInfo(name = "trip_count") val tripCount: Int,
    @ColumnInfo(name = "last_played") val lastPlayed: LocalDate?,
)
