package com.getmecookies.licenseplatequest.ui.xr

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.xr.compose.subspace.SpatialColumn
import androidx.xr.compose.subspace.SpatialPanel
import androidx.xr.compose.subspace.SpatialRow
import androidx.xr.compose.subspace.layout.SubspaceModifier
import androidx.xr.compose.subspace.layout.height
import androidx.xr.compose.subspace.layout.padding
import androidx.xr.compose.subspace.layout.width
import com.getmecookies.licenseplatequest.domain.Achievement
import com.getmecookies.licenseplatequest.ui.screens.passport.achievementMeta

/**
 * A spatial "trophy shelf" for Android XR (experimental): each earned achievement floats as its own
 * small [SpatialPanel], laid out in a grid of [PER_ROW] per row. This is a *subspace* composable —
 * call it inside a `Subspace { }`. Only earned achievements appear (locked ones aren't trophies);
 * render nothing when none are earned.
 *
 * Upgrade path (documented in XR.md): swap each panel for a real glTF trophy mesh loaded via
 * SceneCore once a `.glb` model exists — the layout/data here stays the same.
 */
@Composable
fun XrTrophyShelf(
    earnedAchievements: Set<String>,
    modifier: SubspaceModifier = SubspaceModifier,
) {
    val trophies = Achievement.entries.filter { it.id in earnedAchievements }
    if (trophies.isEmpty()) return

    SpatialColumn(modifier = modifier) {
        trophies.chunked(PER_ROW).forEach { rowTrophies ->
            SpatialRow {
                rowTrophies.forEach { achievement ->
                    SpatialPanel(
                        modifier = SubspaceModifier
                            .width(200.dp)
                            .height(150.dp)
                            .padding(8.dp),
                    ) {
                        TrophyTile(achievement)
                    }
                }
            }
        }
    }
}

@Composable
private fun TrophyTile(achievement: Achievement) {
    val meta = achievementMeta(achievement)
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = meta.icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp),
            )
            Text(
                text = stringResource(meta.titleRes),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                textAlign = TextAlign.Center,
                maxLines = 2,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

private const val PER_ROW = 4
