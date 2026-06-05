package com.getmecookies.licenseplatequest.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.getmecookies.licenseplatequest.R
import com.getmecookies.licenseplatequest.domain.model.RegionOption
import java.util.UUID

/**
 * Shared region (state) picker (playtest #7). A read-only field that opens a searchable bottom
 * sheet — replacing the cramped inline dropdowns on the trip forms. [excludeId] hides one option
 * (e.g. so origin and destination can't pick the same state). Reused for origin, destination, and
 * any future stop fields; the sheet itself is [RegionPickerSheet].
 *
 * Deferred for now (wait on multi-country data): country-filter chips, per-row flags, and a
 * recently-used section.
 */
@Composable
fun RegionPickerField(
    label: String,
    options: List<RegionOption>,
    selectedId: UUID?,
    onSelected: (UUID) -> Unit,
    modifier: Modifier = Modifier,
    excludeId: UUID? = null,
    isError: Boolean = false,
) {
    var showSheet by remember { mutableStateOf(false) }
    val selected = options.firstOrNull { it.id == selectedId }

    Box(modifier = modifier) {
        OutlinedTextField(
            value = selected?.code ?: "",
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            isError = isError,
            trailingIcon = { Icon(Icons.Filled.ArrowDropDown, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
        )
        // A read-only field doesn't reliably take clicks, so an invisible overlay opens the sheet.
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable { showSheet = true },
        )
    }

    if (showSheet) {
        RegionPickerSheet(
            options = options,
            selectedId = selectedId,
            excludeId = excludeId,
            onSelected = {
                onSelected(it)
                showSheet = false
            },
            onDismiss = { showSheet = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegionPickerSheet(
    options: List<RegionOption>,
    selectedId: UUID?,
    onSelected: (UUID) -> Unit,
    onDismiss: () -> Unit,
    excludeId: UUID? = null,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var query by remember { mutableStateOf("") }

    val filtered = remember(options, query, excludeId) {
        val q = query.trim()
        options.filter { option ->
            option.id != excludeId &&
                (
                    q.isEmpty() ||
                        option.name.contains(q, ignoreCase = true) ||
                        option.code.contains(q, ignoreCase = true)
                )
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.region_picker_title),
                style = MaterialTheme.typography.titleMedium,
            )
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                singleLine = true,
                placeholder = { Text(stringResource(R.string.region_picker_search)) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(
                                Icons.Filled.Close,
                                contentDescription = stringResource(R.string.region_picker_clear_search),
                            )
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )

            if (filtered.isEmpty()) {
                Text(
                    text = stringResource(R.string.region_picker_no_match, query.trim()),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 16.dp),
                )
            } else {
                LazyColumn(modifier = Modifier.heightIn(max = 420.dp)) {
                    items(filtered, key = { it.id }) { option ->
                        RegionRow(
                            option = option,
                            selected = option.id == selectedId,
                            onClick = { onSelected(option.id) },
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun RegionRow(
    option: RegionOption,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = option.name,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = option.code,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        // Reserve the check slot so rows don't shift when selection changes.
        Box(modifier = Modifier.size(24.dp), contentAlignment = Alignment.Center) {
            if (selected) {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = stringResource(R.string.region_picker_selected_cd),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}
