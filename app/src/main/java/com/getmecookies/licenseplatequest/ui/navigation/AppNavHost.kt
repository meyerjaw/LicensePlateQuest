package com.getmecookies.licenseplatequest.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.getmecookies.licenseplatequest.ui.screens.celebration.CelebrationScreen
import com.getmecookies.licenseplatequest.ui.screens.players.AddPlayerScreen
import com.getmecookies.licenseplatequest.ui.screens.players.PlayersScreen
import com.getmecookies.licenseplatequest.ui.screens.statedetail.StateDetailScreen
import com.getmecookies.licenseplatequest.ui.screens.trips.NewTripScreen
import com.getmecookies.licenseplatequest.ui.screens.trips.TripsTab

/**
 * Root composable: a [Scaffold] with bottom-nav tabs and the navigation graph. Tabs come
 * from [TopDestination]; full-screen pushes (Add Player, New Trip, State Detail, Celebration)
 * come from [Routes] and hide the bottom bar so they read as their own screen.
 */
@Composable
fun AppRoot() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val currentRoute = currentDestination?.route

    // Full-screen routes that should not show the bottom navigation bar.
    val fullScreenRoutes = setOf(
        Routes.ADD_PLAYER,
        Routes.NEW_TRIP,
        Routes.STATE_DETAIL,
        Routes.CELEBRATION,
    )
    val showBottomBar = currentRoute !in fullScreenRoutes

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    TopDestination.entries.forEach { destination ->
                        val selected = currentDestination
                            ?.hierarchy
                            ?.any { it.route == destination.route } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(destination.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(destination.icon, contentDescription = destination.label) },
                            label = { Text(destination.label) },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = TopDestination.START.route,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(TopDestination.Trips.route) {
                TripsTab(
                    onNewTrip = { navController.navigate(Routes.NEW_TRIP) },
                    onOpenState = { code -> navController.navigate(Routes.stateDetail(code)) },
                    onCelebrate = { tripId, mode ->
                        navController.navigate(Routes.celebration(tripId.toString(), mode.name))
                    },
                )
            }
            composable(TopDestination.Players.route) {
                // Back from the Players tab returns to the Trips tab (the app's home), rather
                // than exiting; the Trips tab itself handles double-back-to-exit.
                BackHandler {
                    navController.navigate(TopDestination.Trips.route) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
                PlayersScreen(
                    onAddPlayer = { navController.navigate(Routes.ADD_PLAYER) },
                )
            }
            composable(Routes.ADD_PLAYER) {
                AddPlayerScreen(
                    onDone = { newId ->
                        if (newId != null) {
                            navController.previousBackStackEntry
                                ?.savedStateHandle
                                ?.set(Routes.RESULT_NEW_PLAYER_ID, newId.toString())
                        }
                        navController.popBackStack()
                    },
                )
            }
            composable(Routes.NEW_TRIP) { entry ->
                val newPlayerId by entry.savedStateHandle
                    .getStateFlow<String?>(Routes.RESULT_NEW_PLAYER_ID, null)
                    .collectAsStateWithLifecycle()
                NewTripScreen(
                    onDone = { navController.popBackStack() },
                    onAddPlayer = { navController.navigate(Routes.ADD_PLAYER) },
                    addedPlayerId = newPlayerId,
                    onAddedPlayerConsumed = {
                        entry.savedStateHandle[Routes.RESULT_NEW_PLAYER_ID] = null
                    },
                )
            }
            composable(
                route = Routes.STATE_DETAIL,
                arguments = listOf(navArgument("code") { type = NavType.StringType }),
            ) {
                StateDetailScreen(
                    onBack = { navController.popBackStack() },
                )
            }
            composable(
                route = Routes.CELEBRATION,
                arguments = listOf(
                    navArgument("tripId") { type = NavType.StringType },
                    navArgument("mode") { type = NavType.StringType },
                ),
            ) {
                CelebrationScreen(
                    onExit = { navController.popBackStack() },
                )
            }
        }
    }
}
