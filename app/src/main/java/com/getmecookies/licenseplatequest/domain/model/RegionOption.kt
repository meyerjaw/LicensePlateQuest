package com.getmecookies.licenseplatequest.domain.model

import java.util.UUID

/**
 * Lightweight option for the origin/destination state dropdowns on the New Trip form.
 * Backed by bundled PlateRegion data so the same canonical id flows into the Trip record.
 */
data class RegionOption(
    val id: UUID,
    val code: String,
    val name: String,
)
