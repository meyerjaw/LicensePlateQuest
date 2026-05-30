package com.getmecookies.licenseplatequest.data.seed

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/**
 * Wire model for the bundled `assets/regions/us_states.json` file (SPEC §8).
 * Field names match the JSON keys exactly. [JsonBundle.dataVersion] drives the
 * compare-and-update seeding so content can ship without schema migrations.
 */
@Serializable
data class RegionBundleDto(
    val schema_version: Int,
    val data_version: Int,
    val country_code: String,
    val notes: String? = null,
    val regions: List<RegionDto>,
)

@Serializable
data class RegionDto(
    val country_code: String,
    val region_code: String,
    val name: String,
    val bird: String,
    val motto: String,
    val flower: String,
    val fun_facts: List<String>,
    val plate_image_path: String,
    val rarity_score: Double,
    val center_lat: Double,
    val center_lng: Double,
    val display_order: Int,
    val additional_info: JsonObject = JsonObject(emptyMap()),
)
