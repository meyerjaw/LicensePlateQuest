package com.getmecookies.licenseplatequest.domain.model

import java.util.UUID

/**
 * The user's saved home location (Settings, playtest note #8): a city plus its state/region.
 * Used to pre-fill the New Trip "From" field. [regionId] points at a PlateRegion.
 */
data class HomeLocation(
    val regionId: UUID,
    val city: String,
)
