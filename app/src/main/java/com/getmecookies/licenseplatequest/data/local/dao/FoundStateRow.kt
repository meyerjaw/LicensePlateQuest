package com.getmecookies.licenseplatequest.data.local.dao

import androidx.room.ColumnInfo
import java.time.Instant

/**
 * Query projection for a found state in the Active Trip View bottom sheet: the state's code,
 * display name, plate image asset path, and when it was spotted.
 */
data class FoundStateRow(
    @ColumnInfo(name = "region_code") val code: String,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "plate_image_path") val plateImagePath: String,
    @ColumnInfo(name = "found_at") val foundAt: Instant,
)
