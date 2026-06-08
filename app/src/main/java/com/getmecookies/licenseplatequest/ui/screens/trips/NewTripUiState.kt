package com.getmecookies.licenseplatequest.ui.screens.trips

import com.getmecookies.licenseplatequest.domain.model.Player
import com.getmecookies.licenseplatequest.domain.model.RegionOption
import java.time.LocalDate
import java.util.UUID

/**
 * One editable stop on the New Trip / Manage trip form (playtest #11). Order is the position in
 * the [NewTripUiState.stops] list — first is the start, last the destination.
 */
data class StopDraft(
    val city: String = "",
    val regionId: UUID? = null,
)

/**
 * State for the full-screen New Trip form (SPEC section 6). The route is an ordered list of
 * [stops] (minimum two: start + destination); intermediate entries are pit stops.
 *
 * [nameManuallyEdited] tracks whether the user has typed their own trip name. While false, the
 * name stays auto-prefilled from the stops + month and updates live (SPEC section 6 prefill rule).
 */
data class NewTripUiState(
    val name: String = "",
    val nameManuallyEdited: Boolean = false,
    val stops: List<StopDraft> = listOf(StopDraft(), StopDraft()),
    val startDate: LocalDate = LocalDate.now(),
    val endDate: LocalDate? = null,
    val regionOptions: List<RegionOption> = emptyList(),
    val allPlayers: List<Player> = emptyList(),
    val selectedPlayerIds: Set<UUID> = emptySet(),
    val saving: Boolean = false,
    val showErrors: Boolean = false,
) {
    /** Whether a stop has both a city and a state (used for per-field error styling). */
    fun stopValid(index: Int): Boolean {
        val stop = stops.getOrNull(index) ?: return false
        return stop.city.isNotBlank() && stop.regionId != null
    }

    /** Resolve a region id to its 2-letter code (for the auto-prefilled name). */
    fun regionCode(regionId: UUID?): String? = regionOptions.firstOrNull { it.id == regionId }?.code

    // Field-level validity (SPEC section 10: name, ≥2 complete stops, at least one player).
    val stopsValid: Boolean
        get() = stops.size >= 2 && stops.all { it.city.isNotBlank() && it.regionId != null }
    val nameValid: Boolean get() = name.isNotBlank()
    val playersValid: Boolean get() = selectedPlayerIds.isNotEmpty()

    val isValid: Boolean get() = nameValid && stopsValid && playersValid
}
