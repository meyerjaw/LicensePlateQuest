package com.getmecookies.licenseplatequest.widget

import android.content.Context
import com.getmecookies.licenseplatequest.LicensePlateQuestApp
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/** Snapshot the home-screen widget renders. Loaded fresh on each update from the active trip. */
sealed interface WidgetTripState {

    /** No trip is active — the widget shows a "start a trip" prompt. */
    data object NoTrip : WidgetTripState

    /** The active trip's at-a-glance progress. */
    data class Active(
        val tripName: String,
        val foundCount: Int,
        val totalStates: Int,
        val foundCodes: Set<String>,
        val lastFoundName: String?,
        val lastFoundAtEpochMs: Long?,
        val dayOfTrip: Int,
    ) : WidgetTripState {
        val remaining: Int get() = (totalStates - foundCount).coerceAtLeast(0)
        val fraction: Float
            get() = if (totalStates <= 0) 0f else foundCount.toFloat() / totalStates
    }
}

const val WIDGET_TOTAL_STATES = 50

/**
 * Build the widget snapshot from the database. Reads the single ACTIVE trip, its found states (with
 * names + timestamps), and derives the day-of-trip — mirroring the in-app map stats strip. Returns
 * [WidgetTripState.NoTrip] when nothing is active.
 */
suspend fun loadWidgetTripState(context: Context): WidgetTripState {
    val container = (context.applicationContext as LicensePlateQuestApp).container
    val trip = container.tripRepository.observeActiveTrip().first() ?: return WidgetTripState.NoTrip

    val game = container.database.gameInstanceDao().getForTrip(trip.id).firstOrNull()
    val rows = game?.let { container.database.spottingDao().getStatRows(it.id) }.orEmpty()
    val last = rows.maxByOrNull { it.timestamp }

    val zone = ZoneId.systemDefault()
    val startDate = trip.createdAt.atZone(zone).toLocalDate()
    val dayOfTrip = (ChronoUnit.DAYS.between(startDate, LocalDate.now(zone)) + 1)
        .toInt().coerceAtLeast(1)

    return WidgetTripState.Active(
        tripName = trip.name,
        foundCount = rows.size,
        totalStates = WIDGET_TOTAL_STATES,
        foundCodes = rows.mapTo(HashSet()) { it.code },
        lastFoundName = last?.name,
        lastFoundAtEpochMs = last?.timestamp?.toEpochMilli(),
        dayOfTrip = dayOfTrip,
    )
}

/**
 * A compact, glanceable "time ago" label for the last find — e.g. "just now", "12 min ago",
 * "3 hr ago", "2 days ago". Pure so it can be unit-tested; [nowMs] is injectable for that.
 */
fun relativeTimeLabel(thenMs: Long, nowMs: Long = System.currentTimeMillis()): String {
    val deltaMs = (nowMs - thenMs).coerceAtLeast(0)
    val minutes = deltaMs / 60_000
    val hours = minutes / 60
    val days = hours / 24
    return when {
        minutes < 1 -> "just now"
        minutes < 60 -> "$minutes min ago"
        hours < 24 -> "$hours hr ago"
        days == 1L -> "yesterday"
        else -> "$days days ago"
    }
}
