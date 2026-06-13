package com.getmecookies.licenseplatequest.domain

/**
 * Analytics seam — a thin, provider-agnostic sink for product analytics (screen views, taps,
 * feature events). Kept Android-free so it's injectable and unit-testable; the real provider
 * implementation (e.g. Firebase) lives in the data layer and is swapped in via [com.getmecookies
 * .licenseplatequest.di.AppContainer]. See `ANALYTICS.md`.
 *
 * Rules for callers:
 * - Names are `snake_case`, ≤ 40 chars; ≤ 25 params per event.
 * - **Never pass PII** — no player/trip names, no city text. Use counts, enums, buckets, and
 *   region codes only.
 */
interface Analytics {
    /** A screen/destination view. [name] is a stable screen key (e.g. "trip_list"). */
    fun screen(name: String, params: Map<String, Any?> = emptyMap())

    /** A discrete event — a tap or a feature action (e.g. "trip_created"). */
    fun event(name: String, params: Map<String, Any?> = emptyMap())

    /** A user-scoped cohort property (non-PII), e.g. "player_count_bucket" = "3-4". */
    fun setUserProperty(name: String, value: String?)
}

/** Sink that drops everything — used in tests, debug, and until a real provider is wired in. */
object NoOpAnalytics : Analytics {
    override fun screen(name: String, params: Map<String, Any?>) = Unit
    override fun event(name: String, params: Map<String, Any?>) = Unit
    override fun setUserProperty(name: String, value: String?) = Unit
}

/**
 * Wraps a real [Analytics] sink and forwards only while the user's analytics consent is on;
 * otherwise every call is a no-op. Consent is read live via [isEnabled] so toggling the Settings
 * switch takes effect immediately. (For an SDK that buffers events, also flip its own collection
 * flag when consent changes — see `ANALYTICS.md`.)
 */
class ConsentGatedAnalytics(
    private val delegate: Analytics,
    private val isEnabled: () -> Boolean,
) : Analytics {
    override fun screen(name: String, params: Map<String, Any?>) {
        if (isEnabled()) delegate.screen(name, params)
    }

    override fun event(name: String, params: Map<String, Any?>) {
        if (isEnabled()) delegate.event(name, params)
    }

    override fun setUserProperty(name: String, value: String?) {
        if (isEnabled()) delegate.setUserProperty(name, value)
    }
}
