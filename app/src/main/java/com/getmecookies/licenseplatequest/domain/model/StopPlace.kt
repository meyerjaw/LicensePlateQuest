package com.getmecookies.licenseplatequest.domain.model

/** A trip stop's state (2-letter [code]) and the [city] the user typed — used to pin the route. */
data class StopPlace(val code: String, val city: String)
