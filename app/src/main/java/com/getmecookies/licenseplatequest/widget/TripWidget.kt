package com.getmecookies.licenseplatequest.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.getmecookies.licenseplatequest.LicensePlateQuestApp
import com.getmecookies.licenseplatequest.MainActivity

/**
 * Home-screen widget for the active trip (X / 50, last find, day of trip, mini filled map). Built
 * with Glance; data is loaded fresh on each update and the map is drawn to a bitmap (Glance can't
 * host the in-app Compose map). Responsive: small shows the count, medium adds last-find/day, large
 * adds the filled map. Tapping anywhere opens the app to the active trip.
 */
class TripWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Responsive(setOf(SMALL, MEDIUM, LARGE))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        // Never let a data/render failure leave the widget stuck on the loading spinner — degrade to
        // the "no trip" card instead (and keep the host process stable).
        val state = runCatching { loadWidgetTripState(context) }
            .getOrDefault(WidgetTripState.NoTrip)
        // Render the filled map once (off the composition) for the large layout.
        val mapBitmapProvider = (state as? WidgetTripState.Active)?.let { active ->
            runCatching {
                val shapes = (context.applicationContext as LicensePlateQuestApp)
                    .container.mapRepository.loadShapes()
                // Keep the bitmap modest — it displays at ~96dp, and oversized bitmaps can exceed the
                // RemoteViews/IPC size limit and make the widget update fail.
                val widthPx = 360
                val heightPx = (widthPx * (shapes.height / shapes.width)).toInt().coerceAtLeast(1)
                ImageProvider(
                    WidgetMapRenderer.render(
                        shapes = shapes,
                        foundCodes = active.foundCodes,
                        widthPx = widthPx,
                        heightPx = heightPx,
                        foundColor = FOUND_ARGB,
                        baseColor = BASE_ARGB,
                        outlineColor = OUTLINE_ARGB,
                    ),
                )
            }.getOrNull()
        }
        provideContent {
            GlanceTheme {
                WidgetSurface {
                    when (state) {
                        WidgetTripState.NoTrip -> NoTripContent()
                        is WidgetTripState.Active -> {
                            val size = LocalSize.current
                            when {
                                size.height >= 180.dp -> LargeContent(state, mapBitmapProvider)
                                size.width >= 200.dp -> MediumContent(state)
                                else -> SmallContent(state)
                            }
                        }
                    }
                }
            }
        }
    }

    companion object {
        // Responsive breakpoints (approx 2x1 / 4x2 / 4x4 home-screen cells).
        private val SMALL = DpSize(120.dp, 60.dp)
        private val MEDIUM = DpSize(250.dp, 110.dp)
        private val LARGE = DpSize(250.dp, 220.dp)

        // Warm "sunny" accent for found states / progress (works on light + dark widget hosts).
        private const val FOUND_ARGB = 0xFFEF9F27.toInt()
        private const val BASE_ARGB = 0x33888780
        private const val OUTLINE_ARGB = 0x55FFFFFF
        val AMBER = Color(0xFFBA7517)
    }
}

@Composable
private fun WidgetSurface(content: @Composable () -> Unit) {
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .background(GlanceTheme.colors.background)
            .cornerRadius(16.dp)
            .clickable(actionStartActivity<MainActivity>())
            .padding(14.dp),
        contentAlignment = Alignment.CenterStart,
    ) { content() }
}

@Composable
private fun SmallContent(state: WidgetTripState.Active) {
    Column(verticalAlignment = Alignment.CenterVertically) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = "${state.foundCount}",
                style = TextStyle(
                    color = GlanceTheme.colors.onBackground,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                ),
            )
            Spacer(GlanceModifier.width(4.dp))
            Text(
                text = "/ ${state.totalStates}",
                style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 15.sp),
            )
        }
        Spacer(GlanceModifier.height(6.dp))
        ProgressBar(state.fraction)
    }
}

@Composable
private fun MediumContent(state: WidgetTripState.Active) {
    Column(modifier = GlanceModifier.fillMaxSize()) {
        TripHeader(state)
        Spacer(GlanceModifier.height(6.dp))
        CountRow(state)
        Spacer(GlanceModifier.height(8.dp))
        ProgressBar(state.fraction)
        Spacer(GlanceModifier.height(8.dp))
        LastFindLine(state)
    }
}

@Composable
private fun LargeContent(state: WidgetTripState.Active, map: ImageProvider?) {
    Column(modifier = GlanceModifier.fillMaxSize()) {
        TripHeader(state)
        Spacer(GlanceModifier.height(8.dp))
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (map != null) {
                Image(
                    provider = map,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = GlanceModifier.size(96.dp),
                )
                Spacer(GlanceModifier.width(14.dp))
            }
            Column(modifier = GlanceModifier.defaultWeight()) {
                CountRow(state)
                Spacer(GlanceModifier.height(8.dp))
                ProgressBar(state.fraction)
                Spacer(GlanceModifier.height(8.dp))
                LastFindLine(state)
            }
        }
    }
}

@Composable
private fun NoTripContent() {
    Column(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "No trip yet",
            style = TextStyle(
                color = GlanceTheme.colors.onBackground,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
            ),
        )
        Spacer(GlanceModifier.height(4.dp))
        Text(
            text = "Start a road trip →",
            style = TextStyle(color = ColorProvider(TripWidget.AMBER), fontSize = 14.sp),
        )
    }
}

@Composable
private fun TripHeader(state: WidgetTripState.Active) {
    Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = state.tripName,
            maxLines = 1,
            style = TextStyle(
                color = ColorProvider(TripWidget.AMBER),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
            ),
            modifier = GlanceModifier.defaultWeight(),
        )
        Text(
            text = "Day ${state.dayOfTrip}",
            style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 12.sp),
        )
    }
}

@Composable
private fun CountRow(state: WidgetTripState.Active) {
    Row(verticalAlignment = Alignment.Bottom) {
        Text(
            text = "${state.foundCount}",
            style = TextStyle(
                color = GlanceTheme.colors.onBackground,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
            ),
        )
        Spacer(GlanceModifier.width(5.dp))
        Text(
            text = "/ ${state.totalStates} states",
            style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 14.sp),
        )
    }
}

@Composable
private fun LastFindLine(state: WidgetTripState.Active) {
    val text = if (state.lastFoundName != null && state.lastFoundAtEpochMs != null) {
        "Last: ${state.lastFoundName} · ${relativeTimeLabel(state.lastFoundAtEpochMs)}"
    } else {
        "${state.remaining} states to go"
    }
    Text(
        text = text,
        maxLines = 1,
        style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 13.sp),
    )
}

@Composable
private fun ProgressBar(fraction: Float) {
    LinearProgressIndicator(
        progress = fraction.coerceIn(0f, 1f),
        modifier = GlanceModifier.fillMaxWidth().height(7.dp),
        color = ColorProvider(TripWidget.AMBER),
        backgroundColor = GlanceTheme.colors.surfaceVariant,
    )
}
