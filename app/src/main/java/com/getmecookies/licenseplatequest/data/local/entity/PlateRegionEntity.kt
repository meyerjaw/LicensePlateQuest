package com.getmecookies.licenseplatequest.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Bundled, static reference data for a platable region (a U.S. state in MVP), loaded from
 * assets on first run (SPEC §8). Generic by design — adding DC, territories, or other
 * countries is just new rows with a different [countryCode]/[regionCode].
 *
 * [funFacts] and [additionalInfo] hold raw JSON strings (a JSON array and a JSON object
 * respectively) so the schema stays stable as content evolves.
 */
@Entity(
    tableName = "plate_region",
    indices = [Index(value = ["country_code", "region_code"], unique = true)],
)
data class PlateRegionEntity(
    @PrimaryKey val id: UUID,
    @ColumnInfo(name = "country_code") val countryCode: String,
    @ColumnInfo(name = "region_code") val regionCode: String,
    val name: String,
    val bird: String,
    val motto: String,
    val flower: String,
    @ColumnInfo(name = "fun_facts") val funFacts: String,
    @ColumnInfo(name = "plate_image_path") val plateImagePath: String,
    @ColumnInfo(name = "rarity_score") val rarityScore: Double,
    @ColumnInfo(name = "center_lat") val centerLat: Double,
    @ColumnInfo(name = "center_lng") val centerLng: Double,
    @ColumnInfo(name = "display_order") val displayOrder: Int,
    @ColumnInfo(name = "additional_info") val additionalInfo: String,
)
