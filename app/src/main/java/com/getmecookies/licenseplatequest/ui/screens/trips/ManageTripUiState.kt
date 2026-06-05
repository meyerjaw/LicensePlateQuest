package com.getmecookies.licenseplatequest.ui.screens.trips

import com.getmecookies.licenseplatequest.domain.model.Player
import com.getmecookies.licenseplatequest.domain.model.RegionOption
import java.time.LocalDate
import java.util.UUID

/**
 * State for the Manage trip (edit) screen (playtest #14). Mirrors [NewTripUiState] but is
 * prefilled from an existing trip and committed via "Save". Unlike New Trip there's no live
 * name auto-prefill — the name is simply an editable field seeded from the trip.
 */
data class ManageTripUiState(
    val loading: Boolean = true,
    val name: String = "",
    val originCity: String = "",
    val originRegionId: UUID? = null,
    val destinationCity: String = "",
    val destinationRegionId: UUID? = null,
    val startDate: LocalDate = LocalDate.now(),
    val endDate: LocalDate? = null,
    val regionOptions: List<RegionOption> = emptyList(),
    val allPlayers: List<Player> = emptyList(),
    val selectedPlayerIds: Set<UUID> = emptySet(),
    val saving: Boolean = false,
    val showErrors: Boolean = false,
) {
    val originRegion: RegionOption? get() = regionOptions.firstOrNull { it.id == originRegionId }
    val destinationRegion: RegionOption? get() = regionOptions.firstOrNull { it.id == destinationRegionId }

    val hasOrigin: Boolean get() = originCity.isNotBlank() || originRegionId != null
    val hasDestination: Boolean get() = destinationCity.isNotBlank() || destinationRegionId != null

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
