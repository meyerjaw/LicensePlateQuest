package com.getmecookies.licenseplatequest.domain.model

import java.time.Instant

/**
 * A state the active trip has found, shown as a row in the Active Trip View bottom sheet.
 */
data class FoundState(
    val code: String,
    val name: String,
    val plateImagePath: String,
    val foundAt: Instant,
)
