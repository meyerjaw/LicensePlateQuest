package com.getmecookies.licenseplatequest.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * A selectable, pill-shaped chip for a player, filled with that player's color (playtest note
 * #19). Selected: the solid color with a contrasting label + check; unselected: a light tint of
 * the same color with a thin colored outline, so the player's identity reads either way. Used
 * everywhere players appear as toggle chips.
 */
@Composable
fun PlayerSelectChip(
    name: String,
    color: Color,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val container = if (selected) color else color.copy(alpha = 0.16f)
    val onContainer = if (selected) {
        if (luminanceOf(color) > 0.6f) Color(0xFF1A1A1A) else Color.White
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(percent = 50),
        color = container,
        contentColor = onContainer,
        border = if (selected) null else BorderStroke(1.dp, color.copy(alpha = 0.5f)),
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            // Always reserve the check slot so selecting doesn't change the pill's width.
            Box(modifier = Modifier.size(18.dp), contentAlignment = Alignment.Center) {
                if (selected) {
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
            Text(text = name, style = MaterialTheme.typography.labelLarge)
        }
    }
}

private fun luminanceOf(color: Color): Float =
    0.299f * color.red + 0.587f * color.green + 0.114f * color.blue
