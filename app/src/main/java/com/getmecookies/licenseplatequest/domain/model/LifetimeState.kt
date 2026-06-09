package com.getmecookies.licenseplatequest.domain.model

import java.time.Instant
import java.util.UUID

/**
 * A state in the lifetime "Plate Passport": its code, name, when it was first ever spotted, and the
 * trip that first caught it (for "first spotted on …" + the new-to-collection highlight).
 */
data class LifetimeState(
    val code: String,
    val name: String,
    val firstFoundAt: Instant,
    val firstTripId: UUID,
    val firstTripName: String,
)
