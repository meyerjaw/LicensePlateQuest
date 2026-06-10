package com.getmecookies.licenseplatequest.data.location

import android.content.Context
import android.location.Geocoder
import com.getmecookies.licenseplatequest.domain.CityLocator
import com.getmecookies.licenseplatequest.domain.GeoPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

/**
 * [CityLocator] backed by the platform [Geocoder]. Runs the (blocking) lookup on IO and degrades
 * gracefully: returns null when no geocoder backend is present or the lookup fails/finds nothing,
 * so callers simply fall back to the state center.
 */
class AndroidCityLocator(context: Context) : CityLocator {

    private val appContext = context.applicationContext
    private val geocoder: Geocoder? =
        if (Geocoder.isPresent()) Geocoder(appContext, Locale.US) else null

    override suspend fun locate(city: String, regionCode: String): GeoPoint? {
        val g = geocoder ?: return null
        val query = "${city.trim()}, $regionCode, USA"
        if (city.isBlank()) return null
        return withContext(Dispatchers.IO) {
            try {
                @Suppress("DEPRECATION") // the async API is API 33+, minSdk is 31
                val results = g.getFromLocationName(query, 1)
                results?.firstOrNull()?.let { GeoPoint(it.latitude, it.longitude) }
            } catch (e: Exception) {
                null
            }
        }
    }
}
