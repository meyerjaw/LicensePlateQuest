package com.getmecookies.licenseplatequest.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import com.getmecookies.licenseplatequest.R
import com.getmecookies.licenseplatequest.ui.PlayerColors

/**
 * A grid of player-color swatches (playtest note #19). The selected swatch shows a ring + check;
 * tapping one reports its token. Shared by Add Player and the edit dialog.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PlayerColorPicker(
    selectedToken: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        PlayerColors.palette.forEach { swatch ->
            val selected = swatch.token == selectedToken
            val cd = stringResource(
                if (selected) R.string.player_color_selected_cd else R.string.player_color_cd,
                swatch.token,
            )
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(swatch.color)
                    .border(
                        width = if (selected) 3.dp else 1.dp,
                        color = if (selected) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.outlineVariant
                        },
                        shape = CircleShape,
                    )
                    .clickable { onSelect(swatch.token) }
                    .clearAndSetSemantics { contentDescription = cd },
                contentAlignment = Alignment.Center,
            ) {
                if (selected) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        tint = checkColorOn(swatch.color),
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}

/** Dark or light check depending on the swatch's brightness, so it always reads. */
private fun checkColorOn(color: Color): Color {
    val luminance = 0.299f * color.red + 0.587f * color.green + 0.114f * color.blue
    return if (luminance > 0.6f) Color(0xFF1A1A1A) else Color.White
}
