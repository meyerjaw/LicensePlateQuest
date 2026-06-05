package com.getmecookies.licenseplatequest.ui.screens.trips

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

/** Validation/derived-state tests for the New Trip and Manage trip UI states. */
class TripFormValidationTest {

    private fun filledNewTrip() = NewTripUiState(
        name = "Summer road trip",
        originCity = "Austin",
        originRegionId = UUID.randomUUID(),
        destinationCity = "Denver",
        destinationRegionId = UUID.randomUUID(),
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
        assertFalse(filledNewTrip().copy(originCity = "").isValid)
        assertFalse(filledNewTrip().copy(originRegionId = null).isValid)
        assertFalse(filledNewTrip().copy(destinationCity = "").isValid)
        assertFalse(filledNewTrip().copy(destinationRegionId = null).isValid)
        assertFalse(filledNewTrip().copy(selectedPlayerIds = emptySet()).isValid)
    }

    @Test
    fun newTrip_hasOrigin_reflectsCityOrRegion() {
        assertFalse(NewTripUiState().hasOrigin)
        assertTrue(NewTripUiState(originCity = "Austin").hasOrigin)
        assertTrue(NewTripUiState(originRegionId = UUID.randomUUID()).hasOrigin)
    }

    @Test
    fun manageTrip_isValid_mirrorsNewTrip() {
        val valid = ManageTripUiState(
            loading = false,
            name = "Edited trip",
            originCity = "Austin",
            originRegionId = UUID.randomUUID(),
            destinationCity = "Denver",
            destinationRegionId = UUID.randomUUID(),
            selectedPlayerIds = setOf(UUID.randomUUID()),
        )
        assertTrue(valid.isValid)
        assertFalse(valid.copy(name = "").isValid)
        assertFalse(valid.copy(selectedPlayerIds = emptySet()).isValid)
    }
}
