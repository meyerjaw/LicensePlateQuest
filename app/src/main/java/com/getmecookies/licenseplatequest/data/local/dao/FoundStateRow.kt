package com.getmecookies.licenseplatequest.data.local.dao

import androidx.room.ColumnInfo
import java.time.Instant

/**
 * Query projection for a found state in the Active Trip View bottom sheet: the state's code,
 * display name, and when it was spotted. The flag image is derived from the code in the UI.
 */
data class FoundStateRow(
    @ColumnInfo(name = "region_code") val code: String,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "found_at") val foundAt: Instant,
)
