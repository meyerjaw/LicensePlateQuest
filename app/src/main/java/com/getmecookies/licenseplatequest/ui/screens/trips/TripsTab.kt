package com.getmecookies.licenseplatequest.ui.screens.trips

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
 * @param onMapViewActiveChange reports whether the Active Trip (map) view is currently showing,
 *   so the host can hide the bottom navigation bar while the map reads as a full-screen view.
 */
@Composable
fun TripsTab(
    onNewTrip: () -> Unit,
    onOpenState: (String) -> Unit,
    onCelebrate: (UUID, CelebrationMode) -> Unit,
    onMapViewActiveChange: (Boolean) -> Unit = {},
    viewModel: TripsTabViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val hasActiveTrip by viewModel.hasActiveTrip.collectAsStateWithLifecycle()

    // When true, show the list even if a trip is active (user tapped "all trips" or pressed back).
    var forceList by rememberSaveable { mutableStateOf(false) }

    // The map is showing when there's an active trip and the user hasn't forced the list. Report
    // it up so the host can hide the bottom bar, and clear it when this tab leaves composition.
    val mapShowing = hasActiveTrip == true && !forceList
    LaunchedEffect(mapShowing) { onMapViewActiveChange(mapShowing) }
    DisposableEffect(Unit) { onDispose { onMapViewActiveChange(false) } }

    // Double-back-to-exit used whenever the Trip List is what's showing.
    val exitOnDoubleBack = rememberDoubleBackToExit()

    when (hasActiveTrip) {
        null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }

        true -> {
            if (forceList) {
                // Trip List is showing (even over an active trip): back double-taps to exit,
                // consistent with the Trip List always being the app's home.
                BackHandler(onBack = exitOnDoubleBack)
                TripListScreen(
                    onNewTrip = onNewTrip,
                    // Selecting a trip activates it; return to the active view.
                    onOpenTrip = { forceList = false },
                    onOpenSummary = { tripId -> onCelebrate(tripId, CelebrationMode.SUMMARY) },
                )
            } else {
                // Active Trip View (the map): back returns to the full Trip List.
                BackHandler { forceList = true }
                ActiveTripScreen(
                    onOpenState = onOpenState,
                    onViewAllTrips = { forceList = true },
                    onCelebrate = onCelebrate,
                )
            }
        }

        false -> {
            // No active trip: always the list. Reset the override so a freshly created/selected
            // trip lands on the active view. Back here double-taps to exit the app.
            forceList = false
            BackHandler(onBack = exitOnDoubleBack)
            TripListScreen(
                onNewTrip = onNewTrip,
                onOpenTrip = { /* selection activates the trip; gateway switches to active view */ },
                onOpenSummary = { tripId -> onCelebrate(tripId, CelebrationMode.SUMMARY) },
            )
        }
    }
}

/** Walk up the Context wrappers to the hosting Activity, if any. */
private fun Context.findActivity(): Activity? {
    var ctx: Context? = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

/**
 * Returns a back action that closes the app only on a second press within 3 seconds; the first
 * press shows a "Press back again to exit" toast. Used on the Trip List (the app's home).
 */
@Composable
private fun rememberDoubleBackToExit(): () -> Unit {
    val context = LocalContext.current
    val lastPress = remember { mutableStateOf(0L) }
    return {
        val now = System.currentTimeMillis()
        if (now - lastPress.value < 3_000L) {
            context.findActivity()?.finish()
        } else {
            lastPress.value = now
            Toast.makeText(context, "Press back again to exit", Toast.LENGTH_SHORT).show()
        }
    }
}
