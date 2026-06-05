package com.getmecookies.licenseplatequest.notifications

import java.time.LocalDate
import java.util.UUID

/**
 * In-memory [ReminderScheduler] test double — records schedule/cancel calls and tracks notified
 * state without touching WorkManager or SharedPreferences.
 */
class FakeReminderScheduler : ReminderScheduler {

    val scheduled = mutableListOf<Pair<UUID, LocalDate>>()
    val cancelled = mutableListOf<UUID>()
    private val notified = mutableMapOf<UUID, LocalDate>()

    val followUps = mutableListOf<Pair<UUID, LocalDate>>()
    val remindedLater = mutableListOf<UUID>()

    override fun scheduleForTrip(tripId: UUID, endDate: LocalDate, replace: Boolean) {
        scheduled += tripId to endDate
    }

    override fun scheduleFollowUp(tripId: UUID, endDate: LocalDate) {
        followUps += tripId to endDate
    }

    override fun remindLater(tripId: UUID) {
        remindedLater += tripId
        notified.remove(tripId)
    }

    override fun cancelForTrip(tripId: UUID) {
        cancelled += tripId
        notified.remove(tripId)
    }

    override fun wasNotifiedFor(tripId: UUID, endDate: LocalDate): Boolean =
        notified[tripId] == endDate

    override fun markNotified(tripId: UUID, endDate: LocalDate) {
        notified[tripId] = endDate
    }

    override fun ensureChannel() {
        // no-op
    }
}
