package com.getmecookies.licenseplatequest.ui.screens.trips

import com.getmecookies.licenseplatequest.domain.model.Player
import com.getmecookies.licenseplatequest.domain.model.RegionOption
import java.time.LocalDate

/**
 * State for the full-screen New Trip form (SPEC section 6). Mirrors the Add Player pattern:
 * its own screen, not a dialog.
 *
 * [nameManuallyEdited] tracks whether the user has typed their own trip name. While false,
 * the name stays auto-prefilled as "Origin to Destination, Month Year" and updates live as
 * origin/destination/date change (SPEC section 6 prefill rule).
 */
data class NewTripUiState(
    val name: String = "",
    val nameManuallyEdited: Boolean = false,
    val originCity: String = "",
    val originRegionId: java.util.UUID? = null,
    val destinationCity: String = "",
    val destinationRegionId: java.util.UUID? = null,
    val startDate: LocalDate = LocalDate.now(),
    val endDate: LocalDate? = null,
    val regionOptions: List<RegionOption> = emptyList(),
    val allPlayers: List<Player> = emptyList(),
    val selectedPlayerIds: Set<java.util.UUID> = emptySet(),
    val saving: Boolean = false,
    val showErrors: Boolean = false,
) {
    // Resolved region for the picked id — used by the name auto-prefill (withPrefilledName).
    val originRegion: RegionOption? get() = regionOptions.firstOrNull { it.id == originRegionId }
    val destinationRegion: RegionOption? get() = regionOptions.firstOrNull { it.id == destinationRegionId }

    // Whether a section has anything to clear (city text or a picked state) — drives the
    // quick-clear control on each section header (playtest note #9).
    val hasOrigin: Boolean get() = originCity.isNotBlank() || originRegionId != null
    val hasDestination: Boolean get() = destinationCity.isNotBlank() || destinationRegionId != null

    // Field-level validity (SPEC section 10: name, origin city+state, destination city+state,
    // start date defaulted, at least one player).
    val originCityValid: Boolean get() = originCity.isNotBlank()
    val originRegionValid: Boolean get() = originRegionId != null
    val destinationCityValid: Boolean get() = destinationCity.isNotBlank()
    val destinationRegionValid: Boolean get() = destinationRegionId != null
    val nameValid: Boolean get() = name.isNotBlank()
    val playersValid: Boolean get() = selectedPlayerIds.isNotEmpty()

    val isValid: Boolean
        get() = nameValid && originCityValid && originRegionValid &&
            destinationCityValid && destinationRegionValid && playersValid
}
