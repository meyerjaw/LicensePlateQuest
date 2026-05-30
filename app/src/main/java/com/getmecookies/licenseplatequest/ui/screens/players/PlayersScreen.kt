package com.getmecookies.licenseplatequest.ui.screens.players

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.getmecookies.licenseplatequest.ui.theme.LicensePlateQuestTheme

/**
 * Players Management — SPEC §6. Foundation renders the empty state; full CRUD arrives in the
 * Player Management milestone.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayersScreen() {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Players") }) },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "Add your first player to get started.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PlayersScreenPreview() {
    LicensePlateQuestTheme {
        PlayersScreen()
    }
}
