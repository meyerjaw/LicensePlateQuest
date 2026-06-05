package com.getmecookies.licenseplatequest.notifications

import java.time.LocalDate
import java.util.UUID

/** No-op [ReminderScheduler] for instrumented tests that don't exercise reminder scheduling. */
object NoopReminderScheduler : ReminderScheduler {
    override fun scheduleForTrip(tripId: UUID, endDate: LocalDate, replace: Boolean) {}
    override fun cancelForTrip(tripId: UUID) {}
    override fun wasNotifiedFor(tripId: UUID, endDate: LocalDate): Boolean = false
    override fun markNotified(tripId: UUID, endDate: LocalDate) {}
    override fun ensureChannel() {}
}
