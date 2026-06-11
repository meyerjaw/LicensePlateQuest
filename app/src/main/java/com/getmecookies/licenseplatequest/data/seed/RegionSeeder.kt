package com.getmecookies.licenseplatequest.data.seed

import android.content.Context
import com.getmecookies.licenseplatequest.data.local.dao.GameTypeDao
import com.getmecookies.licenseplatequest.data.local.dao.PlateRegionDao
import com.getmecookies.licenseplatequest.data.local.entity.GameTypeEntity
import com.getmecookies.licenseplatequest.data.local.entity.PlateRegionEntity
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Loads bundled reference data into the database on first run and on content updates
 * (SPEC §8). Compares the bundled `data_version` against the last-applied value stored in
 * SharedPreferences; only re-applies when the bundle is newer. Idempotent — regions use
 * deterministic ids and [PlateRegionDao.upsertAll], so repeated runs are safe.
 *
 * Also ensures the single MVP [GameTypeEntity] (`license_plate`) exists, since every trip
 * will create a GameInstance referencing it.
 */
class RegionSeeder(
    private val context: Context,
    private val plateRegionDao: PlateRegionDao,
    private val gameTypeDao: GameTypeDao,
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun seedIfNeeded() {
        ensureDefaultGameType()

        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val appliedVersion = prefs.getInt(KEY_DATA_VERSION, -1)

        val bundle = readBundle()
        // Re-seed when the version is current AND the regions are actually present. The version flag
        // (SharedPreferences) and the region table can diverge — e.g. the table is cleared but the
        // flag isn't (a partial data reset, or an in-memory DB in tests) — and an empty map would
        // leave the app unusable.
        if (appliedVersion >= bundle.data_version && plateRegionDao.count() > 0) return

        val entities = bundle.regions.map { it.toEntity() }
        plateRegionDao.upsertAll(entities)
        prefs.edit().putInt(KEY_DATA_VERSION, bundle.data_version).apply()
    }

    private fun readBundle(): RegionBundleDto {
        val text = context.assets.open(ASSET_PATH).bufferedReader().use { it.readText() }
        return json.decodeFromString(RegionBundleDto.serializer(), text)
    }

    private fun RegionDto.toEntity(): PlateRegionEntity = PlateRegionEntity(
        id = Ids.region(country_code, region_code),
        countryCode = country_code,
        regionCode = region_code,
        name = name,
        bird = bird,
        motto = motto,
        flower = flower,
        funFacts = json.encodeToString(fun_facts),
        plateImagePath = plate_image_path,
        rarityScore = rarity_score,
        centerLat = center_lat,
        centerLng = center_lng,
        displayOrder = display_order,
        additionalInfo = json.encodeToString(additional_info),
    )

    private suspend fun ensureDefaultGameType() {
        if (gameTypeDao.getByCode(LICENSE_PLATE_CODE) != null) return
        gameTypeDao.upsert(
            GameTypeEntity(
                id = Ids.gameType(LICENSE_PLATE_CODE),
                code = LICENSE_PLATE_CODE,
                name = "License Plate Game",
                description = "Spot license plates from each U.S. state and collect all 50.",
            ),
        )
    }

    companion object {
        const val LICENSE_PLATE_CODE = "license_plate"
        private const val ASSET_PATH = "regions/us_states.json"
        private const val PREFS = "region_seed"
        private const val KEY_DATA_VERSION = "data_version"
    }
}
