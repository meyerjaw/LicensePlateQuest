package com.getmecookies.licenseplatequest.domain.model

/**
 * Lightweight identity for a state (code + display name), used to list every state — found or
 * not — in the Active Trip bottom sheet. Full facts live in [StateInfo]; found timing in
 * [FoundState].
 */
data class StateSummary(
    val code: String,
    val name: String,
)
