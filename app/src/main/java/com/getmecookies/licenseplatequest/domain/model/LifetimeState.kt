package com.getmecookies.licenseplatequest.domain.model

import java.time.Instant

/** A state in the lifetime "Plate Passport": its code, name, and when it was first ever spotted. */
data class LifetimeState(
    val code: String,
    val name: String,
    val firstFoundAt: Instant,
)
