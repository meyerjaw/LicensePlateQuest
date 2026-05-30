package com.getmecookies.licenseplatequest.ui.screens.trips

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.getmecookies.licenseplatequest.ui.AppViewModelProvider
import com.getmecookies.licenseplatequest.ui.theme.LicensePlateQuestTheme

/**
 * Trip List (Home) — SPEC section 6. The FAB opens the full-screen New Trip flow. The
 * sectioned Active / In Progress / Completed list arrives in the Trip List milestone; for
 * now this shows the empty state plus a count confirming created trips persist.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripListScreen(
    onNewTrip: () -> Unit,
    viewModel: TripListViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val tripCount by viewModel.tripCount.collectAsStateWithLifecycle()
    val regionCount by viewModel.regionCount.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Trips") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = onNewTrip) {
                Icon(Icons.Filled.Add, contentDescription = "New trip")
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = if (tripCount == 0) {
                        "No trips yet — tap + to start your first one."
                    } else {
                        "$tripCount trip(s) saved"
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = if (regionCount > 0) {
                        "$regionCount states loaded - ready to play"
                    } else {
                        "Loading state data..."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TripListScreenPreview() {
    LicensePlateQuestTheme {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No trips yet — tap + to start your first one.")
        }
    }
}
