package com.getmecookies.licenseplatequest.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.UUID

/** Pure-logic tests for the derived properties on [TripListItem] (overdue, completion, duration). */
class TripListItemTest {

    private fun item(
        status: TripStatus = TripStatus.ACTIVE,
        startDate: LocalDate = LocalDate.now(),
        endDate: LocalDate? = null,
        endedAt: Instant? = null,
        createdAt: Instant = Instant.now(),
        foundCount: Int = 0,
    ) = TripListItem(
        id = UUID.randomUUID(),
        name = "Test trip",
        status = status,
        startDate = startDate,
        endDate = endDate,
        endedAt = endedAt,
        createdAt = createdAt,
        foundCount = foundCount,
    )

    @Test
    fun isOverdue_falseWhenNoEndDate() {
        assertFalse(item(endDate = null).isOverdue)
    }

    @Test
    fun isOverdue_trueWhenPastEndAndNotCompleted() {
        assertTrue(item(status = TripStatus.ACTIVE, endDate = LocalDate.now().minusDays(1)).isOverdue)
        assertTrue(item(status = TripStatus.IN_PROGRESS, endDate = LocalDate.now().minusDays(3)).isOverdue)
    }

    @Test
    fun isOverdue_falseWhenCompletedEvenIfPast() {
        assertFalse(item(status = TripStatus.COMPLETED, endDate = LocalDate.now().minusDays(5)).isOverdue)
    }

    @Test
    fun isOverdue_falseWhenEndIsTodayOrFuture() {
        assertFalse(item(endDate = LocalDate.now()).isOverdue)
        assertFalse(item(endDate = LocalDate.now().plusDays(1)).isOverdue)
    }

    @Test
    fun isComplete_trueAtFiftyStates() {
        assertTrue(item(foundCount = 50).isComplete)
        assertFalse(item(foundCount = 49).isComplete)
    }

    @Test
    fun durationLabel_nullWhenNotEnded() {
        assertNull(item(endedAt = null).durationLabel)
    }

    @Test
    fun durationLabel_formatsDaysHoursMinutes() {
        val created = Instant.parse("2026-06-01T00:00:00Z")
        assertEquals("2 days", item(createdAt = created, endedAt = created.plus(2, ChronoUnit.DAYS)).durationLabel)
        assertEquals("1 day", item(createdAt = created, endedAt = created.plus(1, ChronoUnit.DAYS)).durationLabel)
        assertEquals("5 hours", item(createdAt = created, endedAt = created.plus(5, ChronoUnit.HOURS)).durationLabel)
        assertEquals("1 hour", item(createdAt = created, endedAt = created.plus(1, ChronoUnit.HOURS)).durationLabel)
        assertEquals("30 minutes", item(createdAt = created, endedAt = created.plus(30, ChronoUnit.MINUTES)).durationLabel)
        assertEquals("1 minute", item(createdAt = created, endedAt = created.plus(1, ChronoUnit.MINUTES)).durationLabel)
    }
}
