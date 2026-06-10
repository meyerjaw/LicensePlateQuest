package com.getmecookies.licenseplatequest.data.local.dao

import androidx.room.ColumnInfo
import java.time.Instant
import java.util.UUID

/** One spotting, projected for achievement stats: which state, when, and on which trip. */
data class SpottingFindRow(
    @ColumnInfo(name = "region_code") val code: String,
    @ColumnInfo(name = "timestamp") val timestamp: Instant,
    @ColumnInfo(name = "trip_id") val tripId: UUID,
)
