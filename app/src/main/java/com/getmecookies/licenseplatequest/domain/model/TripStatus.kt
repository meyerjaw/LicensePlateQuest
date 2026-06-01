package com.getmecookies.licenseplatequest.domain.model

import com.getmecookies.licenseplatequest.domain.model.TripStatus.ACTIVE
import com.getmecookies.licenseplatequest.domain.model.TripStatus.COMPLETED
import com.getmecookies.licenseplatequest.domain.model.TripStatus.IN_PROGRESS


/**
 * Lifecycle status of a [com.getmecookies.licenseplatequest.data.local.entity.TripEntity].
 *
 * Invariant (SPEC §7): at most one trip is [ACTIVE] at a time. Setting a trip active
 * demotes the previous active trip to [IN_PROGRESS]. A trip becomes [COMPLETED] only on
 * a manual end — never automatically on reaching 50/50.
 *
 * [wire] is the stable string persisted in the database and used in JSON payloads, so it
 * must not change even if the enum constant names are refactored.
 */
enum class TripStatus(val wire: String) {
    ACTIVE("active"),
    IN_PROGRESS("in_progress"),
    COMPLETED("completed");

    companion object {
        fun fromWire(value: String): TripStatus =
            entries.firstOrNull { it.wire == value }
                ?: error("Unknown TripStatus wire value: $value")
    }
}
