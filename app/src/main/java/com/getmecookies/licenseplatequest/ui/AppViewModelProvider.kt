package com.getmecookies.licenseplatequest.ui

import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.getmecookies.licenseplatequest.LicensePlateQuestApp
import com.getmecookies.licenseplatequest.ui.screens.players.AddPlayerViewModel
import com.getmecookies.licenseplatequest.ui.screens.players.PlayersViewModel
import com.getmecookies.licenseplatequest.ui.screens.trips.NewTripViewModel
import com.getmecookies.licenseplatequest.ui.screens.trips.TripListViewModel

/**
 * Central place that builds ViewModels by hand (no DI framework in MVP). Each ViewModel
 * pulls its dependencies from the application's [com.getmecookies.licenseplatequest.di.AppContainer].
 */
object AppViewModelProvider {
    val Factory = viewModelFactory {
        initializer {
            TripListViewModel(
                tripRepository = lpqApp().container.tripRepository,
                regionRepository = lpqApp().container.regionRepository,
            )
        }
        initializer {
            PlayersViewModel(lpqApp().container.playerRepository)
        }
        initializer {
            AddPlayerViewModel(lpqApp().container.playerRepository)
        }
        initializer {
            NewTripViewModel(
                tripRepository = lpqApp().container.tripRepository,
                regionRepository = lpqApp().container.regionRepository,
                playerRepository = lpqApp().container.playerRepository,
            )
        }
    }
}

/** Convenience accessor for the typed Application from within a ViewModel factory. */
fun CreationExtras.lpqApp(): LicensePlateQuestApp =
    this[APPLICATION_KEY] as LicensePlateQuestApp
