package com.getmecookies.licenseplatequest.data.analytics

import android.content.Context
import android.os.Bundle
import com.getmecookies.licenseplatequest.domain.Analytics
import com.google.firebase.analytics.FirebaseAnalytics

/**
 * Firebase implementation of the [Analytics] seam (see ANALYTICS.md §5). Maps the provider-agnostic
 * calls onto [FirebaseAnalytics]. Consent is honored two ways: the `ConsentGatedAnalytics` wrapper
 * drops our explicit calls when the user opts out, and [setCollectionEnabled] also flips Firebase's
 * automatic collection (sessions, etc.) so an opt-out is complete.
 *
 * The SDK handle is obtained defensively: if no default `FirebaseApp` is initialized (e.g. JVM unit
 * tests with no `google-services.json` processing), the client degrades to a silent no-op instead of
 * crashing app startup.
 */
class FirebaseAnalyticsClient(context: Context) : Analytics {

    private val firebase: FirebaseAnalytics? = runCatching {
        FirebaseAnalytics.getInstance(context.applicationContext)
    }.getOrNull()

    override fun screen(name: String, params: Map<String, Any?>) {
        val bundle = analyticsParamsToBundle(params)
        bundle.putString(FirebaseAnalytics.Param.SCREEN_NAME, name)
        firebase?.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundle)
    }

    override fun event(name: String, params: Map<String, Any?>) {
        firebase?.logEvent(name, analyticsParamsToBundle(params))
    }

    override fun setUserProperty(name: String, value: String?) {
        firebase?.setUserProperty(name, value)
    }

    /** Turn Firebase's automatic + manual collection on/off — the consent opt-out switch. */
    fun setCollectionEnabled(enabled: Boolean) {
        firebase?.setAnalyticsCollectionEnabled(enabled)
    }
}

/**
 * Convert a seam params map to a Firebase [Bundle]. Firebase accepts String/Long/Double param
 * values; ints/floats are widened, booleans become "true"/"false" strings (readable in DebugView),
 * nulls are dropped, and anything else falls back to its string form. Top-level + internal so the
 * mapping is unit-testable without a live FirebaseApp.
 */
internal fun analyticsParamsToBundle(params: Map<String, Any?>): Bundle = Bundle().apply {
    params.forEach { (key, value) ->
        when (value) {
            null -> Unit
            is String -> putString(key, value)
            is Int -> putLong(key, value.toLong())
            is Long -> putLong(key, value)
            is Double -> putDouble(key, value)
            is Float -> putDouble(key, value.toDouble())
            is Boolean -> putString(key, value.toString())
            else -> putString(key, value.toString())
        }
    }
}
