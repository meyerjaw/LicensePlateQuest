package com.getmecookies.licenseplatequest.ui.screens.statedetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.getmecookies.licenseplatequest.domain.model.StateDetailData
import com.getmecookies.licenseplatequest.ui.AppViewModelProvider
import com.getmecookies.licenseplatequest.ui.components.FlagImage
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * State Detail (SPEC section 6). Shows the state's bundled facts and plate image. When the
 * active trip hasn't found it, offers "Mark as found" (commits immediately, then returns to
 * the map); when found, shows the found timestamp and trip and offers "Unmark" (confirmed).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StateDetailScreen(
    onBack: () -> Unit,
    viewModel: StateDetailViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val data = uiState.data

    // Marking commits immediately and returns to the map.
    LaunchedEffect(uiState.markComplete) {
        if (uiState.markComplete) onBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(data?.info?.name ?: "State") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center,
        ) {
            when {
                uiState.loading -> CircularProgressIndicator()
                data == null -> Text("State not found.")
                else -> StateDetailContent(
                    data = data,
                    onMark = viewModel::onMarkClick,
                    onUnmark = viewModel::onUnmarkClick,
                )
            }
        }
    }

    when (uiState.dialog) {
        StateDetailDialog.NONE -> Unit
        StateDetailDialog.CONFIRM_UNMARK -> ConfirmDialog(
            title = "Unmark this state?",
            body = "Remove ${data?.info?.name ?: "this state"} from this trip? This can't be undone.",
            confirmLabel = "Unmark",
            onConfirm = viewModel::onConfirmUnmark,
            onDismiss = viewModel::onDismissDialog,
        )
    }
}

@Composable
private fun StateDetailContent(
    data: StateDetailData,
    onMark: () -> Unit,
    onUnmark: () -> Unit,
) {
    val info = data.info
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Flag, framed so it reads as a single hero element regardless of flag shape.
        Surface(
            shape = RoundedCornerShape(12.dp),
            tonalElevation = 1.dp,
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth(),
        ) {
            FlagImage(
                code = info.code,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
            )
        }

        // When-found banner sits up top once a state is in the trip.
        if (data.found) {
            FoundBanner(data)
        }

        SectionCard(title = "State symbols") {
            FactRow(label = "State bird", value = info.bird)
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            FactRow(label = "State flower", value = info.flower)
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            FactRow(label = "Motto", value = info.motto)
        }

        if (info.funFacts.isNotEmpty()) {
            SectionCard(title = "Fun facts") {
                info.funFacts.forEachIndexed { index, fact ->
                    if (index > 0) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("•", style = MaterialTheme.typography.bodyMedium)
                        Text(fact, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }

        when {
            data.found -> {
                OutlinedButton(onClick = onUnmark, modifier = Modifier.fillMaxWidth()) {
                    Text("Unmark")
                }
            }
            data.hasActiveTrip -> {
                Button(onClick = onMark, modifier = Modifier.fillMaxWidth()) {
                    Text("Mark as found")
                }
            }
            else -> {
                Text(
                    "Start or select a trip to mark states.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** A titled card section with a subtle background, used to group related rows. */
@Composable
private fun SectionCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
        )
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                content = content,
            )
        }
    }
}

@Composable
private fun FactRow(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(text = value, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun FoundBanner(data: StateDetailData) {
    val whenText = data.foundAt?.let {
        DATE_FORMAT.format(it.atZone(ZoneId.systemDefault()))
    }
    val parts = buildList {
        if (whenText != null) add("Found $whenText")
        data.foundTripName?.let { add("on “$it”") }
    }
    if (parts.isNotEmpty()) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = parts.joinToString(" "),
                style = MaterialTheme.typography.bodyMedium,
                fontStyle = FontStyle.Italic,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            )
        }
    }
}

@Composable
private fun ConfirmDialog(
    title: String,
    body: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(body) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(confirmLabel) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

private val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a")
