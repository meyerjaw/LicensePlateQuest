package com.getmecookies.licenseplatequest.ui.screens.trips

import com.getmecookies.licenseplatequest.domain.model.Player
import com.getmecookies.licenseplatequest.domain.model.RegionOption
import java.time.LocalDate
import java.util.UUID

/**
 * State for the Manage trip (edit) screen (playtest #14). Mirrors [NewTripUiState]'s stops model
 * (playtest #11) but is prefilled from an existing trip and committed via "Save". There's no live
 * name auto-prefill — the name is simply an editable field seeded from the trip.
 */
data class ManageTripUiState(
    val loading: Boolean = true,
    val name: String = "",
    val stops: List<StopDraft> = listOf(StopDraft(), StopDraft()),
    val startDate: LocalDate = LocalDate.now(),
    val endDate: LocalDate? = null,
    val regionOptions: List<RegionOption> = emptyList(),
    val allPlayers: List<Player> = emptyList(),
    val selectedPlayerIds: Set<UUID> = emptySet(),
    val saving: Boolean = false,
    val showErrors: Boolean = false,
    /** Shown when saving a non-completed trip with an end date in the past (playtest #14). */
    val showEndTripPrompt: Boolean = false,
) {
    fun stopValid(index: Int): Boolean {
        val stop = stops.getOrNull(index) ?: return false
        return stop.city.isNotBlank() && stop.regionId != null
    }

    val stopsValid: Boolean
        get() = stops.size >= 2 && stops.all { it.city.isNotBlank() && it.regionId != null }
    val nameValid: Boolean get() = name.isNotBlank()
    val playersValid: Boolean get() = selectedPlayerIds.isNotEmpty()

    val isValid: Boolean get() = nameValid && stopsValid && playersValid
}
