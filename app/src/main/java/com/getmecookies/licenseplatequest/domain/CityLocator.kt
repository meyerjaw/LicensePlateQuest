package com.getmecookies.licenseplatequest.domain

/** A geographic coordinate (WGS84 degrees). */
data class GeoPoint(val lat: Double, val lng: Double)

/**
 * Resolves a typed city (within a US state) to coordinates, so the route can pin the real city
 * rather than the state's center (playtest #11 follow-up). Implementations may use the device
 * geocoder; callers fall back to the state center when this returns null.
 */
interface CityLocator {
    suspend fun locate(city: String, regionCode: String): GeoPoint?
}
