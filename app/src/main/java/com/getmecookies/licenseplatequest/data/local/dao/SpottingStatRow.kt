package com.getmecookies.licenseplatequest.data.local.dao

import androidx.room.ColumnInfo
import java.time.Instant

/**
 * Query projection for celebration-stats computation: each spotting joined to its region's
 * name, geographic center, and rarity score, plus when it was found.
 */
data class SpottingStatRow(
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "center_lat") val centerLat: Double,
    @ColumnInfo(name = "center_lng") val centerLng: Double,
    @ColumnInfo(name = "rarity_score") val rarityScore: Double,
    @ColumnInfo(name = "timestamp") val timestamp: Instant,
)
