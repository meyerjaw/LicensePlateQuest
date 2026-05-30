package com.getmecookies.licenseplatequest.data.local.dao

import androidx.room.ColumnInfo
import androidx.room.Embedded
import com.getmecookies.licenseplatequest.data.local.entity.TripEntity

/**
 * Query projection: a trip plus its number of found states ([foundCount]), computed by
 * counting distinct spotted regions across the trip's game instances. Drives the Trip List
 * rows (X / 50 progress) without loading every spotting.
 */
data class TripListRow(
    @Embedded val trip: TripEntity,
    @ColumnInfo(name = "found_count") val foundCount: Int,
)
