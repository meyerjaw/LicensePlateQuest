package com.getmecookies.licenseplatequest.domain.model

import java.util.UUID

/**
 * A single stop on a trip's route (playtest #11). Order is given by the position in the list it
 * lives in — the first is the start, the last the final destination.
 */
data class TripStop(
    val regionId: UUID,
    val city: String,
)
