package com.getmecookies.licenseplatequest.ui.xr

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.getmecookies.licenseplatequest.data.map.MapRepository
import com.getmecookies.licenseplatequest.ui.map.UsMap
import com.getmecookies.licenseplatequest.ui.map.UsMapShapes

/**
 * Content for the dedicated, big "curved map" spatial panel on Android XR (experimental). Shows the
 * active trip's filled US map, display-only, reflecting finds live. Reuses the in-app [UsMap]; only
 * the hosting panel differs from the phone UI.
 */
@Composable
fun XrMapPanel(
    mapRepository: MapRepository,
    foundCodes: Set<String>,
    modifier: Modifier = Modifier,
) {
    val shapes by produceState<UsMapShapes?>(initialValue = null, mapRepository) {
        value = mapRepository.loadShapes()
    }
    Surface(modifier = modifier, color = MaterialTheme.colorScheme.surfaceVariant) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            val current = shapes
            if (current != null) {
                UsMap(
                    shapes = current,
                    foundCodes = foundCodes,
                    onStateClick = {},
                    interactive = false,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(current.width / current.height),
                )
            } else {
                CircularProgressIndicator()
            }
        }
    }
}
