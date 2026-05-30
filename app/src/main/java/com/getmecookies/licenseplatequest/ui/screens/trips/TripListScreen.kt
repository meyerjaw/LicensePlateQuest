package com.getmecookies.licenseplatequest.ui.screens.trips

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
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
 * Trip List (Home) — SPEC §6. Foundation renders the empty state plus a small status line
 * confirming bundled region data loaded. The sectioned Active / In Progress / Completed
 * list and the "+" FAB arrive in the Trip List milestone.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripListScreen(
    viewModel: TripListViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val regionCount by viewModel.regionCount.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Trips") }) },
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
                    text = "No trips yet — tap + to start your first one.",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = if (regionCount > 0) {
                        "$regionCount states loaded · ready to play"
                    } else {
                        "Loading state data…"
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
        // Preview without a ViewModel-backed count.
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No trips yet — tap + to start your first one.")
        }
    }
}
