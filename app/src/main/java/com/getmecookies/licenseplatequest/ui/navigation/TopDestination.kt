package com.getmecookies.licenseplatequest.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Public
import androidx.compose.ui.graphics.vector.ImageVector
import com.getmecookies.licenseplatequest.R

/**
 * The top-level tabs in the bottom navigation (SPEC §5): Trips, Passport, Players.
 * More screens (State Detail, New Trip, celebrations) are nested destinations added in
 * later milestones rather than tabs.
 */
enum class TopDestination(
    val route: String,
    @StringRes val labelRes: Int,
    val icon: ImageVector,
) {
    Trips("trips", R.string.nav_trips, Icons.AutoMirrored.Filled.List),
    Passport("passport", R.string.nav_passport, Icons.Filled.Public),
    Players("players", R.string.nav_players, Icons.Filled.People);

    companion object {
        val START = Trips
    }
}
