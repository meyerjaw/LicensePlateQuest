package com.getmecookies.licenseplatequest.ui

import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.getmecookies.licenseplatequest.LicensePlateQuestApp
import com.getmecookies.licenseplatequest.ui.screens.activetrip.ActiveTripViewModel
import com.getmecookies.licenseplatequest.ui.screens.celebration.CelebrationViewModel
import com.getmecookies.licenseplatequest.ui.screens.onboarding.OnboardingViewModel
import com.getmecookies.licenseplatequest.ui.screens.passport.PassportViewModel
import com.getmecookies.licenseplatequest.ui.screens.players.AddPlayerViewModel
import com.getmecookies.licenseplatequest.ui.screens.players.PlayersViewModel
import com.getmecookies.licenseplatequest.ui.screens.settings.SettingsViewModel
import com.getmecookies.licenseplatequest.ui.screens.statedetail.StateDetailViewModel
import com.getmecookies.licenseplatequest.ui.screens.trips.ManageTripViewModel
import com.getmecookies.licenseplatequest.ui.screens.trips.NewTripViewModel
import com.getmecookies.licenseplatequest.ui.screens.trips.TripListViewModel
import com.getmecookies.licenseplatequest.ui.screens.trips.TripsTabViewModel

/**
 * Central place that builds ViewModels by hand (no DI framework in MVP). Each ViewModel
 * pulls its dependencies from the application's [com.getmecookies.licenseplatequest.di.AppContainer].
 */
object AppViewModelProvider {
    val Factory = viewModelFactory {
        initializer {
            TripListViewModel(
                tripRepository = lpqApp().container.tripRepository,
            )
        }
        initializer {
            TripsTabViewModel(
                tripRepository = lpqApp().container.tripRepository,
            )
        }
        initializer {
            ActiveTripViewModel(
                mapRepository = lpqApp().container.mapRepository,
                tripRepository = lpqApp().container.tripRepository,
                spottingRepository = lpqApp().container.spottingRepository,
                regionRepository = lpqApp().container.regionRepository,
                achievementRepository = lpqApp().container.achievementRepository,
                cityLocator = lpqApp().container.cityLocator,
                celebrationTracker = lpqApp().container.celebrationTracker,
                uiPreferences = lpqApp().container.uiPreferences,
                settingsRepository = lpqApp().container.settingsRepository,
                analytics = lpqApp().container.analytics,
            )
        }
        initializer {
            CelebrationViewModel(
                savedStateHandle = createSavedStateHandle(),
                celebrationRepository = lpqApp().container.celebrationRepository,
                tripRepository = lpqApp().container.tripRepository,
                mapRepository = lpqApp().container.mapRepository,
            )
        }
        initializer {
            PlayersViewModel(lpqApp().container.playerRepository)
        }
        initializer {
            PassportViewModel(
                mapRepository = lpqApp().container.mapRepository,
                spottingRepository = lpqApp().container.spottingRepository,
                tripRepository = lpqApp().container.tripRepository,
                regionRepository = lpqApp().container.regionRepository,
                achievementRepository = lpqApp().container.achievementRepository,
            )
        }
        initializer {
            AddPlayerViewModel(lpqApp().container.playerRepository)
        }
        initializer {
            NewTripViewModel(
                tripRepository = lpqApp().container.tripRepository,
                regionRepository = lpqApp().container.regionRepository,
                playerRepository = lpqApp().container.playerRepository,
                settingsRepository = lpqApp().container.settingsRepository,
                analytics = lpqApp().container.analytics,
            )
        }
        initializer {
            StateDetailViewModel(
                savedStateHandle = createSavedStateHandle(),
                spottingRepository = lpqApp().container.spottingRepository,
                analytics = lpqApp().container.analytics,
            )
        }
        initializer {
            ManageTripViewModel(
                savedStateHandle = createSavedStateHandle(),
                tripRepository = lpqApp().container.tripRepository,
                regionRepository = lpqApp().container.regionRepository,
                playerRepository = lpqApp().container.playerRepository,
            )
        }
        initializer {
            SettingsViewModel(
                settingsRepository = lpqApp().container.settingsRepository,
                regionRepository = lpqApp().container.regionRepository,
                sampleDataSeeder = lpqApp().container.sampleDataSeeder,
                uiPreferences = lpqApp().container.uiPreferences,
            )
        }
        initializer {
            OnboardingViewModel(
                uiPreferences = lpqApp().container.uiPreferences,
                settingsRepository = lpqApp().container.settingsRepository,
                playerRepository = lpqApp().container.playerRepository,
                tripRepository = lpqApp().container.tripRepository,
                regionRepository = lpqApp().container.regionRepository,
            )
        }
    }
}

/** Convenience accessor for the typed Application from within a ViewModel factory. */
fun CreationExtras.lpqApp(): LicensePlateQuestApp =
    this[APPLICATION_KEY] as LicensePlateQuestApp
