package com.getmecookies.licenseplatequest.ui.screens.trips

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.getmecookies.licenseplatequest.ui.AppViewModelProvider
import com.getmecookies.licenseplatequest.ui.screens.activetrip.ActiveTripScreen
import com.getmecookies.licenseplatequest.ui.screens.celebration.CelebrationMode
import java.util.UUID

/**
 * Gateway for the Trips tab (SPEC section 5): shows the Active Trip View when a trip is
 * active, otherwise the Trip List. The user can also explicitly jump to the full list from
 * the active view (the "all trips" action) via [forceList]; selecting a trip there clears it.
 *
 * @param onNewTrip open the New Trip flow.
 * @param onOpenState open a state's detail (from map tap or found-states sheet).
 */
@Composable
fun TripsTab(
    onNewTrip: () -> Unit,
    onOpenState: (String) -> Unit,
    onCelebrate: (UUID, CelebrationMode) -> Unit,
    viewModel: TripsTabViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val hasActiveTrip by viewModel.hasActiveTrip.collectAsStateWithLifecycle()

    // When true, show the list even if a trip is active (user tapped "all trips").
    var forceList by rememberSaveable { mutableStateOf(false) }

    when (hasActiveTrip) {
        null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }

        true -> {
            if (forceList) {
                TripListScreen(
                    onNewTrip = onNewTrip,
                    // Selecting a trip activates it; return to the active view.
                    onOpenTrip = { forceList = false },
                )
            } else {
                ActiveTripScreen(
                    onOpenState = onOpenState,
                    onViewAllTrips = { forceList = true },
                    onCelebrate = onCelebrate,
                )
            }
        }

        false -> {
            // No active trip: always the list. Reset the override so a freshly created/selected
            // trip lands on the active view.
            forceList = false
            TripListScreen(
                onNewTrip = onNewTrip,
                onOpenTrip = { /* selection activates the trip; gateway switches to active view */ },
            )
        }
    }
}
