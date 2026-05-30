package com.getmecookies.licenseplatequest.ui

import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.getmecookies.licenseplatequest.LicensePlateQuestApp
import com.getmecookies.licenseplatequest.ui.screens.trips.TripListViewModel

/**
 * Central place that builds ViewModels by hand (no DI framework in MVP). Each ViewModel
 * pulls its dependencies from the application's [com.getmecookies.licenseplatequest.di.AppContainer].
 */
object AppViewModelProvider {
    val Factory = viewModelFactory {
        initializer {
            TripListViewModel(lpqApp().container.regionRepository)
        }
    }
}

/** Convenience accessor for the typed Application from within a ViewModel factory. */
fun CreationExtras.lpqApp(): LicensePlateQuestApp =
    this[APPLICATION_KEY] as LicensePlateQuestApp
