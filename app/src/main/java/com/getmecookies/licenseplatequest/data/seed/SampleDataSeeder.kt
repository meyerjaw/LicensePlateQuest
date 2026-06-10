package com.getmecookies.licenseplatequest.data.seed

import androidx.room.withTransaction
import com.getmecookies.licenseplatequest.data.local.AppDatabase
import com.getmecookies.licenseplatequest.data.repository.AchievementRepository
import com.getmecookies.licenseplatequest.data.repository.PlayerRepository
import com.getmecookies.licenseplatequest.data.repository.RegionRepository
import com.getmecookies.licenseplatequest.data.repository.SpottingRepository
import com.getmecookies.licenseplatequest.data.repository.TripRepository
import com.getmecookies.licenseplatequest.domain.model.TripStatus
import com.getmecookies.licenseplatequest.domain.model.TripStop
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

/**
 * Debug-only sample-data builder. Populates a believable, varied dataset so a fresh install is one
 * tap from exercising every surface: a roster of players with distinct colors, several **completed**
 * trips (including a 50/50 sweep that lights up the "completed map" styling and fills the Passport),
 * a couple of **in-progress** trips (one overdue), and the **active** trip — each with its own finds,
 * mixed single/multi-player/unattributed credit, and back-dated timestamps so durations, the trip
 * date ranges, the Passport's first-spotted dates, and the recap all read like real history.
 *
 * Built on the normal repositories (so it goes through the real create/mark/end paths), with direct
 * DAO writes only to back-date timestamps that the public API always stamps as "now".
 */
class SampleDataSeeder(
    private val database: AppDatabase,
    private val regionRepository: RegionRepository,
    private val playerRepository: PlayerRepository,
    private val tripRepository: TripRepository,
    private val spottingRepository: SpottingRepository,
    private val achievementRepository: AchievementRepository,
    private val regionSeeder: RegionSeeder,
) {
    private val tripDao = database.tripDao()
    private val gameInstanceDao = database.gameInstanceDao()
    private val spottingDao = database.spottingDao()
    private val zone: ZoneId = ZoneId.systemDefault()

    /** One credited (or unattributed) find within a sample trip. */
    private data class Find(val code: String, val credited: List<UUID>)

    /**
     * Debug-only: erase all user-generated data — trips (FK-cascading to stops, players-on-trip,
     * games, and spottings), the player roster, earned achievements, and the event log — while
     * keeping the bundled reference data (regions + game types). Returns a short result summary.
     */
    suspend fun wipeAllData(): String = try {
        database.withTransaction {
            database.tripDao()
                .deleteAll() // cascades: game_instance -> spotting -> spotting_player, trip_stop, trip_player
            database.playerDao().deleteAll()
            database.achievementDao().deleteAll()
            database.eventLogDao().deleteAll()
        }
        "All trips, players, and progress wiped"
    } catch (e: Exception) {
        "Wipe failed: ${e.javaClass.simpleName} ${e.message ?: ""}".trim()
    }

    /**
     * Build the full sample dataset. Returns a short human-readable summary (the debug Toast shows
     * it). Safe to tap on a fresh install — it ensures regions are seeded first.
     */
    suspend fun seed(): String {
        return try {
            regionSeeder.seedIfNeeded()
            val regionsByCode = regionRepository.getAllRegions().associateBy { it.regionCode }
            if (regionsByCode.size < 2) {
                return "Seed failed: only ${regionsByCode.size} regions loaded"
            }
            val allCodes = regionsByCode.values.sortedBy { it.displayOrder }.map { it.regionCode }

            // Roster: distinct palette colors so attribution chips/leaderboard read clearly.
            val alex = playerRepository.addPlayer("Alex", "blue")
            val sam = playerRepository.addPlayer("Sam", "green")
            val jordan = playerRepository.addPlayer("Jordan", "orange")
            val riley = playerRepository.addPlayer("Riley", "purple")
            val casey = playerRepository.addPlayer("Casey", "pink")
            val dana = playerRepository.addPlayer("Dana", "teal")

            fun stops(vararg pairs: Pair<String, String>): List<TripStop> =
                pairs.mapNotNull { (code, city) ->
                    regionsByCode[code]?.let { TripStop(it.id, city) }
                }

            // Completed — a full 50/50 sweep ~a year ago (completed-map styling + fills Passport).
            seedTrip(
                name = "Cross-Country Champions",
                stops = stops("CA" to "Los Angeles", "NY" to "New York"),
                players = listOf(alex, sam, jordan, riley, casey, dana),
                finds = attribute(allCodes, listOf(alex, sam, jordan, riley, casey, dana)),
                status = TripStatus.COMPLETED,
                startDaysAgo = 360,
                durationDays = 30,
            )
            // Completed — Pacific Coast, ~6 months ago.
            seedTrip(
                name = "Pacific Coast Highway",
                stops = stops("WA" to "Seattle", "CA" to "San Diego"),
                players = listOf(alex, sam, riley),
                finds = attribute(
                    listOf("WA", "OR", "CA", "NV", "AZ", "ID", "UT", "MT"),
                    listOf(alex, sam, riley),
                ),
                status = TripStatus.COMPLETED,
                startDaysAgo = 180,
                durationDays = 12,
            )
            // Completed — Route 66, ~3 months ago.
            seedTrip(
                name = "Route 66 Adventure",
                stops = stops("IL" to "Chicago", "CA" to "Santa Monica"),
                players = listOf(alex, jordan, casey, dana),
                finds = attribute(
                    listOf("IL", "MO", "KS", "OK", "TX", "NM", "AZ", "CA"),
                    listOf(alex, jordan, casey, dana),
                ),
                status = TripStatus.COMPLETED,
                startDaysAgo = 90,
                durationDays = 9,
            )
            // In-progress + overdue — ended 3 days ago but never wrapped up.
            seedTrip(
                name = "Southwest Loop",
                stops = stops("TX" to "El Paso", "CO" to "Denver"),
                players = listOf(sam, casey),
                finds = attribute(listOf("TX", "NM", "AZ", "CO", "UT"), listOf(sam, casey)),
                status = TripStatus.IN_PROGRESS,
                startDaysAgo = 10,
                durationDays = 7, // end date = 3 days ago -> overdue
            )
            // In-progress — currently running, end date in the future.
            seedTrip(
                name = "Great Lakes Tour",
                stops = stops("MI" to "Detroit", "WI" to "Milwaukee"),
                players = listOf(jordan, riley, dana),
                finds = attribute(
                    listOf("MI", "OH", "IN", "IL", "WI"),
                    listOf(jordan, riley, dana)
                ),
                status = TripStatus.IN_PROGRESS,
                startDaysAgo = 6,
                durationDays = 10, // end date = +4 days
            )
            // Active — the trip you're playing now (multi-stop), a few fresh finds.
            seedTrip(
                name = "Sample Road Trip",
                stops = stops(
                    "OH" to "Columbus",
                    "KY" to "Louisville",
                    "TN" to "Nashville",
                    "FL" to "Orlando",
                ),
                players = listOf(alex, sam, jordan),
                finds = listOf(
                    Find("OH", listOf(alex)),
                    Find("KY", listOf(alex, sam)),
                    Find("TN", listOf(jordan)),
                ),
                status = TripStatus.ACTIVE,
                startDaysAgo = 2,
                durationDays = 7, // end date = +5 days
            )

            // Pre-evaluate achievements so the Passport shows earned badges immediately.
            achievementRepository.evaluateAndPersist()

            "Sample data added: 6 players, 6 trips (3 completed, 2 in-progress, 1 active)"
        } catch (e: Exception) {
            "Seed failed: ${e.javaClass.simpleName} ${e.message ?: ""}".trim()
        }
    }

    /**
     * Create a trip (which makes it active), mark its finds against it, then set its final status,
     * dates, and back-dated timestamps. Because each trip is active only while being marked, the
     * "one active trip" rule holds; the final ACTIVE trip is simply the last one seeded.
     */
    private suspend fun seedTrip(
        name: String,
        stops: List<TripStop>,
        players: List<UUID>,
        finds: List<Find>,
        status: TripStatus,
        startDaysAgo: Long,
        durationDays: Long,
    ) {
        if (stops.size < 2) return
        val start = LocalDate.now().minusDays(startDaysAgo)
        val end = start.plusDays(durationDays)

        val tripId = tripRepository.createTrip(
            name = name,
            stops = stops,
            startDate = start,
            endDate = null, // set below via DAO so no overdue reminder is scheduled for sample data
            playerIds = players,
        )
        finds.forEach { spottingRepository.markState(it.code, it.credited) }

        // Back-date the trip window + timestamps, and stamp the final status.
        val createdAt = start.atTime(9, 0).atZone(zone).toInstant()
        val endedInstant = end.atTime(18, 0).atZone(zone).toInstant()
        tripDao.getById(tripId)?.let { trip ->
            tripDao.update(
                trip.copy(
                    startDate = start,
                    endDate = end,
                    status = status,
                    endedAt = if (status == TripStatus.COMPLETED) endedInstant else null,
                    createdAt = createdAt,
                    updatedAt = endedInstant,
                ),
            )
        }

        // Spread the finds' timestamps across the trip window so first-spotted dates, the recap
        // timeline, and "busiest day" all have realistic spacing.
        val gameId = gameInstanceDao.getForTrip(tripId).firstOrNull()?.id ?: return
        val spottings = spottingDao.observeForGame(gameId).first()
        val windowMinutes = (durationDays.coerceAtLeast(1)) * 24 * 60
        spottings.forEachIndexed { index, spotting ->
            val offset = if (spottings.size <= 1) 0L else windowMinutes * index / (spottings.size)
            val ts = createdAt.plusSeconds(offset * 60)
            spottingDao.backdateSpotting(spotting.id, ts)
        }
    }

    /**
     * Assign credit across [players] in a repeating pattern that yields single-player, two-player,
     * and the occasional unattributed find — exercising the leaderboard and the "Family Find" row.
     */
    private fun attribute(codes: List<String>, players: List<UUID>): List<Find> =
        codes.mapIndexed { i, code ->
            val credited = when {
                players.isEmpty() -> emptyList()
                i % 5 == 4 -> emptyList() // every 5th find is unattributed
                i % 4 == 3 -> listOf(players[i % players.size], players[(i + 1) % players.size])
                else -> listOf(players[i % players.size])
            }
            Find(code, credited)
        }
}
