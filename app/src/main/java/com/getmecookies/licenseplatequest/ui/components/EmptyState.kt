package com.getmecookies.licenseplatequest.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * A friendly placeholder for an empty list screen — a large illustration (or tinted [icon]) above
 * centered [message] copy. Prefer [illustrationRes] (a full-color vector, e.g. the road-trip van)
 * for a warmer feel; [icon] is the simpler tinted fallback. Used on the empty Trip List and
 * Players roster instead of bare text.
 */
@Composable
fun EmptyState(
    message: String,
    modifier: Modifier = Modifier,
    @DrawableRes illustrationRes: Int? = null,
    icon: ImageVector? = null,
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        when {
            illustrationRes != null -> Image(
                painter = painterResource(illustrationRes),
                contentDescription = null,
                modifier = Modifier.size(160.dp),
            )

            icon != null -> Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(72.dp),
            )
        }
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
