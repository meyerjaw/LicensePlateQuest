package com.getmecookies.licenseplatequest.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

/**
 * Room migration tests (instrumented — MigrationTestHelper runs on a device). Guards against
 * data-eating or schema-mismatching migrations: the v3→v4 case asserts existing trip data
 * survives and gets seeded into trip_stop, and the full chain validates every step's schema
 * against the exported JSONs.
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java,
    )

    @Test
    fun migrate3To4_preservesTrip_andSeedsStops() {
        val tripId = UUID.randomUUID().toString()
        val originId = UUID.randomUUID().toString()
        val destId = UUID.randomUUID().toString()

        helper.createDatabase(TEST_DB, 3).apply {
            execSQL(insertRegion(originId, "TX", "Texas", order = 1))
            execSQL(insertRegion(destId, "CO", "Colorado", order = 2))
            execSQL(
                "INSERT INTO `trip` (`id`, `name`, `origin_city`, `origin_region_id`, " +
                    "`destination_city`, `destination_region_id`, `start_date`, `end_date`, " +
                    "`status`, `ended_at`, `created_at`, `updated_at`) VALUES (" +
                    "'$tripId', 'Road Trip', 'Austin', '$originId', 'Denver', '$destId', " +
                    "'2026-06-01', NULL, 'active', NULL, " +
                    "'2026-06-01T00:00:00Z', '2026-06-01T00:00:00Z')",
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(TEST_DB, 4, true, MIGRATION_3_4)

        // The trip still exists.
        db.query("SELECT `name` FROM `trip` WHERE `id` = '$tripId'").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("Road Trip", cursor.getString(0))
        }

        // Two stops were seeded, in order, from origin and destination.
        db.query(
            "SELECT `position`, `region_id`, `city` FROM `trip_stop` " +
                "WHERE `trip_id` = '$tripId' ORDER BY `position`",
        ).use { cursor ->
            assertEquals(2, cursor.count)

            assertTrue(cursor.moveToNext())
            assertEquals(0, cursor.getInt(0))
            assertEquals(originId, cursor.getString(1))
            assertEquals("Austin", cursor.getString(2))

            assertTrue(cursor.moveToNext())
            assertEquals(1, cursor.getInt(0))
            assertEquals(destId, cursor.getString(1))
            assertEquals("Denver", cursor.getString(2))
        }
    }

    @Test
    fun migrate4To5_addsCelebratedAtColumn() {
        helper.createDatabase(TEST_DB, 4).close()

        val db = helper.runMigrationsAndValidate(TEST_DB, 5, true, MIGRATION_4_5)

        db.query("PRAGMA table_info(`spotting`)").use { cursor ->
            val nameIdx = cursor.getColumnIndex("name")
            val columns = buildSet { while (cursor.moveToNext()) add(cursor.getString(nameIdx)) }
            assertTrue("celebrated_at column was not added", columns.contains("celebrated_at"))
        }
    }

    @Test
    fun allMigrations_validateAgainstExportedSchemas() {
        helper.createDatabase(TEST_DB, 1).close()
        helper.runMigrationsAndValidate(
            TEST_DB, 5, true, MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5,
        )
    }

    private fun insertRegion(id: String, code: String, name: String, order: Int): String =
        "INSERT INTO `plate_region` (`id`, `country_code`, `region_code`, `name`, `bird`, " +
            "`motto`, `flower`, `fun_facts`, `plate_image_path`, `rarity_score`, `center_lat`, " +
            "`center_lng`, `display_order`, `additional_info`) VALUES (" +
            "'$id', 'US', '$code', '$name', '', '', '', '[]', '', 0.0, 0.0, 0.0, $order, '{}')"

    private companion object {
        const val TEST_DB = "migration-test"
    }
}
