package com.getmecookies.licenseplatequest.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * One stop on a trip's route (playtest #11). Trips are an ordered list of stops: the first is the
 * start, the last is the final destination, and any in between are pit stops. [position] is the
 * 0-based order. The legacy `Trip.origin_*`/`destination_*` columns are kept in sync with the
 * first and last stops so existing stats and prefill keep working.
 */
@Entity(
    tableName = "trip_stop",
    foreignKeys = [
        ForeignKey(
            entity = TripEntity::class,
            parentColumns = ["id"],
            childColumns = ["trip_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = PlateRegionEntity::class,
            parentColumns = ["id"],
            childColumns = ["region_id"],
        ),
    ],
    indices = [
        Index("trip_id"),
        Index("region_id"),
    ],
)
data class TripStopEntity(
    @PrimaryKey val id: UUID,
    @ColumnInfo(name = "trip_id") val tripId: UUID,
    val position: Int,
    @ColumnInfo(name = "region_id") val regionId: UUID,
    val city: String,
)
