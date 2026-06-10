package com.getmecookies.licenseplatequest.data.repository

import com.getmecookies.licenseplatequest.data.local.AppDatabase
import com.getmecookies.licenseplatequest.data.local.entity.AchievementEntity
import com.getmecookies.licenseplatequest.domain.AchievementStats
import com.getmecookies.licenseplatequest.domain.evaluateAchievements
import com.getmecookies.licenseplatequest.domain.model.TripStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.ZoneId

/**
 * Earned achievements (playtest: achievements). Builds an [AchievementStats] snapshot from existing
 * data, evaluates the pure catalog, and persists newly-earned ones (earned-once). No new writes to
 * gameplay tables — it's a read over spottings/trips plus the small `achievement` table.
 */
class AchievementRepository(
    private val database: AppDatabase,
    private val regionRepository: RegionRepository,
) {
    private val spottingDao = database.spottingDao()
    private val tripDao = database.tripDao()
    private val achievementDao = database.achievementDao()

    /** The set of earned achievement ids, observed for the Passport's achievements section. */
    fun observeEarned(): Flow<Set<String>> = achievementDao.observeEarnedIds().map { it.toSet() }

    /** Build the current progress snapshot from the database. */
    suspend fun getStats(): AchievementStats {
        val zone = ZoneId.systemDefault()
        val finds = spottingDao.getAllFinds()

        val lifetimeFound = finds.mapTo(HashSet()) { it.code }
        val maxStatesInOneDay = finds
            .groupBy({ it.timestamp.atZone(zone).toLocalDate() }, { it.code })
            .values.maxOfOrNull { it.toSet().size } ?: 0
        val earliestFindHour = finds.minOfOrNull { it.timestamp.atZone(zone).hour }
        val reachedFiftyOnATrip = finds
            .groupBy({ it.tripId }, { it.code })
            .values.any { it.toSet().size >= 50 }

        val tripRows = tripDao.getStatusPlayerCounts()
        val completed = tripRows.filter { it.status == TripStatus.COMPLETED }

        return AchievementStats(
            lifetimeFound = lifetimeFound,
            allRareCodes = regionRepository.getRareCodes(),
            completedTripCount = completed.size,
            reachedFiftyOnATrip = reachedFiftyOnATrip,
            maxStatesInOneDay = maxStatesInOneDay,
            maxPlayersOnCompletedTrip = completed.maxOfOrNull { it.playerCount } ?: 0,
            earliestFindHour = earliestFindHour,
        )
    }

    /** Re-evaluate, persist any newly-earned achievements, and return the ids just unlocked. */
    suspend fun evaluateAndPersist(): Set<String> {
        val earnedNow = evaluateAchievements(getStats())
        val already = achievementDao.getEarnedIds().toSet()
        val newly = earnedNow - already
        if (newly.isNotEmpty()) {
            val now = Instant.now()
            achievementDao.insertIgnore(newly.map { AchievementEntity(it, now) })
        }
        return newly
    }
}
