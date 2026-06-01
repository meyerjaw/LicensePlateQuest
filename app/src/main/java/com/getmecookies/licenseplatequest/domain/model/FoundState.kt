package com.getmecookies.licenseplatequest.domain.model

import java.time.Instant

/**
 * A state the active trip has found, shown as a row in the Active Trip View bottom sheet. The
 * flag image path is derived from [code] (flags/<code>.png) in the UI, so it isn't stored here.
 */
data class FoundState(
    val code: String,
    val name: String,
    val foundAt: Instant,
)
