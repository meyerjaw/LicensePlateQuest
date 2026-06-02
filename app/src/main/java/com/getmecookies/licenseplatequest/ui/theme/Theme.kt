package com.getmecookies.licenseplatequest.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColors = lightColorScheme(
    primary = SkyBlue,
    onPrimary = Color.White,
    primaryContainer = SkyContainer,
    onPrimaryContainer = OnSkyContainer,

    secondary = GrassGreen,
    onSecondary = Color.White,
    secondaryContainer = GrassContainer,
    onSecondaryContainer = OnGrassContainer,

    tertiary = SunOrange,
    onTertiary = Color.White,
    tertiaryContainer = SunContainer,
    onTertiaryContainer = OnSunContainer,

    error = Coral,
    onError = Color.White,
    errorContainer = CoralContainer,
    onErrorContainer = OnCoralContainer,

    background = WarmCream,
    onBackground = OnWarmLight,
    surface = WarmSurface,
    onSurface = OnWarmLight,
    surfaceVariant = WarmSurfaceVariant,
    onSurfaceVariant = OnWarmVariantLight,
    outline = WarmOutline,
)

private val DarkColors = darkColorScheme(
    primary = SkyBlueLight,
    onPrimary = OnSkyContainer,
    primaryContainer = SkyBlueDark,
    onPrimaryContainer = SkyContainer,

    secondary = GrassGreenLight,
    onSecondary = OnGrassContainer,
    secondaryContainer = GrassContainerDark,
    onSecondaryContainer = GrassContainer,

    tertiary = SunOrangeLight,
    onTertiary = OnSunContainer,
    tertiaryContainer = SunContainerDark,
    onTertiaryContainer = SunContainer,

    error = CoralLight,
    onError = OnCoralContainer,
    errorContainer = Color(0xFF8C2A12),
    onErrorContainer = CoralContainer,

    background = WarmDarkBg,
    onBackground = OnWarmDark,
    surface = WarmDarkSurface,
    onSurface = OnWarmDark,
    surfaceVariant = WarmDarkSurfaceVariant,
    onSurfaceVariant = OnWarmVariantDark,
    outline = WarmDarkOutline,
)

/**
 * App theme. Uses the fixed "sunny road-trip" brand palette by default so the playful look is
 * consistent on every device. Material You dynamic color can be opted back in via [dynamicColor]
 * (off by default — it would otherwise override the brand colors with the system wallpaper).
 */
@Composable
fun LicensePlateQuestTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        shapes = AppShapes,
        typography = AppTypography,
        content = content,
    )
}
