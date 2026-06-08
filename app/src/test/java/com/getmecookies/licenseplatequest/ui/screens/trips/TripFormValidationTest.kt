package com.getmecookies.licenseplatequest.ui.screens.trips

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

/** Validation/derived-state tests for the New Trip and Manage trip UI states. */
class TripFormValidationTest {

    private fun filledNewTrip() = NewTripUiState(
        name = "Summer road trip",
        stops = listOf(
            StopDraft("Austin", UUID.randomUUID()),
            StopDraft("Denver", UUID.randomUUID()),
        ),
        selectedPlayerIds = setOf(UUID.randomUUID()),
    )

    @Test
    fun newTrip_isValid_whenAllFieldsPresent() {
        assertTrue(filledNewTrip().isValid)
    }

    @Test
    fun newTrip_invalid_whenAnyRequiredFieldMissing() {
        assertFalse(NewTripUiState().isValid)
        assertFalse(filledNewTrip().copy(name = "").isValid)
        assertFalse(filledNewTrip().copy(selectedPlayerIds = emptySet()).isValid)
        // Fewer than two stops.
        assertFalse(filledNewTrip().copy(stops = listOf(StopDraft("Austin", UUID.randomUUID()))).isValid)
        // A stop missing its city.
        assertFalse(
            filledNewTrip().copy(
                stops = listOf(StopDraft("", UUID.randomUUID()), StopDraft("Denver", UUID.randomUUID())),
            ).isValid,
        )
        // A stop missing its region.
        assertFalse(
            filledNewTrip().copy(
                stops = listOf(StopDraft("Austin", null), StopDraft("Denver", UUID.randomUUID())),
            ).isValid,
        )
    }

    @Test
    fun newTrip_stopsValid_requiresTwoCompleteStops() {
        assertTrue(filledNewTrip().stopsValid)
        // Default state has two empty stops.
        assertFalse(NewTripUiState().stopsValid)
    }

    @Test
    fun manageTrip_isValid_mirrorsNewTrip() {
        val valid = ManageTripUiState(
            loading = false,
            name = "Edited trip",
            stops = listOf(
                StopDraft("Austin", UUID.randomUUID()),
                StopDraft("Denver", UUID.randomUUID()),
            ),
            selectedPlayerIds = setOf(UUID.randomUUID()),
        )
        assertTrue(valid.isValid)
        assertFalse(valid.copy(name = "").isValid)
        assertFalse(valid.copy(selectedPlayerIds = emptySet()).isValid)
        assertFalse(valid.copy(stops = listOf(StopDraft("Austin", UUID.randomUUID()))).isValid)
    }
}
