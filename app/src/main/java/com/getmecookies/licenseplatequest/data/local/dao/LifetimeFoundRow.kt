package com.getmecookies.licenseplatequest.data.local.dao

import androidx.room.ColumnInfo
import java.time.Instant

/**
 * Query projection for the lifetime "Plate Passport": a state collected across *all* trips, with
 * the date it was first ever spotted. The flag image is derived from the code in the UI.
 */
data class LifetimeFoundRow(
    @ColumnInfo(name = "region_code") val code: String,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "first_found_at") val firstFoundAt: Instant,
)
