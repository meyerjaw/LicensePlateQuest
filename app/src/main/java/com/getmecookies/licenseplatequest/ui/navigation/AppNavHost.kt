package com.getmecookies.licenseplatequest.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.getmecookies.licenseplatequest.ui.screens.players.AddPlayerScreen
import com.getmecookies.licenseplatequest.ui.screens.players.PlayersScreen
import com.getmecookies.licenseplatequest.ui.screens.trips.TripListScreen

/**
 * Root composable: a [Scaffold] with bottom-nav tabs and the navigation graph. Tabs come
 * from [TopDestination]; full-screen pushes (like Add Player) come from [Routes] and hide
 * the bottom bar so they read as their own screen.
 */
@Composable
fun AppRoot() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val currentRoute = currentDestination?.route

    // Full-screen routes that should not show the bottom navigation bar.
    val showBottomBar = currentRoute != Routes.ADD_PLAYER

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
            composable(TopDestination.Trips.route) { TripListScreen() }
            composable(TopDestination.Players.route) {
                PlayersScreen(
                    onAddPlayer = { navController.navigate(Routes.ADD_PLAYER) },
                )
            }
            composable(Routes.ADD_PLAYER) {
                AddPlayerScreen(
                    onDone = { navController.popBackStack() },
                )
            }
        }
    }
}
