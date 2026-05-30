package com.getmecookies.licenseplatequest.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.getmecookies.licenseplatequest.domain.model.TripStatus
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

/**
 * A single road trip (SPEC §7). Origin/destination reference [PlateRegionEntity] rows so
 * the state dropdowns and distance math reuse the same canonical data.
 */
@Entity(
    tableName = "trip",
    foreignKeys = [
        ForeignKey(
            entity = PlateRegionEntity::class,
            parentColumns = ["id"],
            childColumns = ["origin_region_id"],
        ),
        ForeignKey(
            entity = PlateRegionEntity::class,
            parentColumns = ["id"],
            childColumns = ["destination_region_id"],
        ),
    ],
    indices = [
        Index("origin_region_id"),
        Index("destination_region_id"),
        Index("status"),
    ],
)
data class TripEntity(
    @PrimaryKey val id: UUID,
    val name: String,
    @ColumnInfo(name = "origin_city") val originCity: String,
    @ColumnInfo(name = "origin_region_id") val originRegionId: UUID,
    @ColumnInfo(name = "destination_city") val destinationCity: String,
    @ColumnInfo(name = "destination_region_id") val destinationRegionId: UUID,
    @ColumnInfo(name = "start_date") val startDate: LocalDate,
    val status: TripStatus,
    @ColumnInfo(name = "ended_at") val endedAt: Instant?,
    @ColumnInfo(name = "created_at") val createdAt: Instant,
    @ColumnInfo(name = "updated_at") val updatedAt: Instant,
)
