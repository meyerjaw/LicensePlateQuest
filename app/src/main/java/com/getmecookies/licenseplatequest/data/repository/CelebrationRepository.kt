package com.getmecookies.licenseplatequest.data.repository

import com.getmecookies.licenseplatequest.data.local.AppDatabase
import com.getmecookies.licenseplatequest.data.local.dao.SpottingStatRow
import com.getmecookies.licenseplatequest.domain.model.CelebrationStats
import java.time.Duration
import java.time.Instant
import java.util.UUID
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Computes the stats shown on the celebration screens (SPEC section 6). Pure read/derive — no
 * writes. Distances use the haversine formula over bundled state-center coordinates.
 */
class CelebrationRepository(
    private val database: AppDatabase,
) {
    private val tripDao = database.tripDao()
    private val gameInstanceDao = database.gameInstanceDao()
    private val spottingDao = database.spottingDao()
    private val plateRegionDao = database.plateRegionDao()
    private val tripPlayerDao = database.tripPlayerDao()

    suspend fun getStats(tripId: UUID): CelebrationStats? {
        val trip = tripDao.getById(tripId) ?: return null
        val game = gameInstanceDao.getForTrip(tripId).firstOrNull()
        val rows: List<SpottingStatRow> = game?.let { spottingDao.getStatRows(it.id) } ?: emptyList()
        val players = tripPlayerDao.getPlayerNamesForTrip(tripId)

        // Trip duration: from when the trip was actually started (the moment "Start trip" was
        // tapped, i.e. createdAt) to its end — or now if still active. Using createdAt rather than
        // the start date's midnight keeps short trips from reading as a full day or more.
        val startInstant = trip.createdAt
        val endInstant = trip.endedAt ?: Instant.now()
        val durationText = formatDuration(Duration.between(startInstant, endInstant))

        // Gaps between consecutive finds (rows are timestamp-ordered).
        val gaps: List<Duration> = rows.zipWithNext { a, b -> Duration.between(a.timestamp, b.timestamp) }
        val averageGapText = gaps.takeIf { it.isNotEmpty() }
            ?.let { formatDuration(Duration.ofMillis(it.sumOf(Duration::toMillis) / it.size)) }
        val longestGapText = gaps.maxByOrNull { it.toMillis() }?.let { formatDuration(it) }
        val shortestGapText = gaps.minByOrNull { it.toMillis() }?.let { formatDuration(it) }

        val firstStateName = rows.firstOrNull()?.name
        val lastStateName = rows.lastOrNull()?.name

        // Estimated distance: sum of consecutive state-center distances, in the found order.
        val estimatedMiles = rows.zipWithNext { a, b ->
            haversineMiles(a.centerLat, a.centerLng, b.centerLat, b.centerLng)
        }.sum()
        val estimatedDistanceText = if (rows.size >= 2) {
            "${"%,d".format(estimatedMiles.roundToInt())} mi"
        } else {
            null
        }

        // Furthest state from the trip origin (by state-center distance).
        val origin = plateRegionDao.getById(trip.originRegionId)
        val furthestStateName = if (origin != null && rows.isNotEmpty()) {
            rows.maxByOrNull {
                haversineMiles(origin.centerLat, origin.centerLng, it.centerLat, it.centerLng)
            }?.name
        } else {
            null
        }

        val rarestStateName = rows.maxByOrNull { it.rarityScore }?.name

        return CelebrationStats(
            tripName = trip.name,
            foundCount = rows.size,
            foundCodes = rows.mapTo(mutableSetOf()) { it.code },
            durationText = durationText,
            averageGapText = averageGapText,
            longestGapText = longestGapText,
            shortestGapText = shortestGapText,
            firstStateName = firstStateName,
            lastStateName = lastStateName,
            estimatedDistanceText = estimatedDistanceText,
            furthestStateName = furthestStateName,
            rarestStateName = rarestStateName,
            playerNames = players,
        )
    }

    private fun formatDuration(d: Duration): String {
        val totalMinutes = d.toMinutes()
        val days = totalMinutes / (60 * 24)
        val hours = (totalMinutes % (60 * 24)) / 60
        val minutes = totalMinutes % 60
        return when {
            days > 0 -> "${days}d ${hours}h"
            hours > 0 -> "${hours}h ${minutes}m"
            totalMinutes > 0 -> "${minutes}m"
            else -> "${d.seconds}s"
        }
    }

    private fun haversineMiles(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val r = 3958.7613 // Earth radius in miles
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLng / 2) * sin(dLng / 2)
        return r * (2 * atan2(sqrt(a), sqrt(1 - a)))
    }
}
