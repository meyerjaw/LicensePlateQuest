package com.getmecookies.licenseplatequest.ui.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.getmecookies.licenseplatequest.R

/** Asset path for a state's flag, derived from its 2-letter code (e.g. "OH" -> "flags/oh.png"). */
fun flagAssetPath(code: String): String = "flags/${code.lowercase()}.png"

/**
 * Shows a state's flag. If the bundled asset at [assetPath] (e.g. "flags/oh.png") exists it is
 * rendered at its true aspect ratio; otherwise a styled placeholder showing the state [code] is
 * drawn. Dropping real PNGs into assets/flags/ later "just works" — no code change needed.
 *
 * Unlike a license plate, flags aren't a uniform shape, so the image keeps its own aspect
 * ratio ([ContentScale.Fit] with wrap-height) rather than being forced into a fixed box. The
 * caller sizes the width via [modifier]; [placeholderFontSize] scales the placeholder text.
 */
@Composable
fun FlagImage(
    code: String,
    modifier: Modifier = Modifier,
    assetPath: String = flagAssetPath(code),
    placeholderFontSize: TextUnit = 44.sp,
) {
    val context = LocalContext.current
    var bitmap by remember(assetPath) { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(assetPath) {
        bitmap = runCatching {
            context.assets.open(assetPath).use { BitmapFactory.decodeStream(it) }?.asImageBitmap()
        }.getOrNull()
    }

    val bmp = bitmap
    if (bmp != null) {
        // Preserve the flag's real proportions; wrap height to whatever the width implies.
        Image(
            bitmap = bmp,
            contentDescription = stringResource(R.string.flag_image_cd, code),
            contentScale = ContentScale.Fit,
            modifier = modifier
                .wrapContentHeight()
                .clip(RoundedCornerShape(4.dp)),
        )
    } else {
        FlagPlaceholder(code = code, fontSize = placeholderFontSize, modifier = modifier)
    }
}

@Composable
private fun FlagPlaceholder(
    code: String,
    fontSize: TextUnit,
    modifier: Modifier = Modifier,
) {
    // 3:2 is the most common US state-flag ratio, so the placeholder roughly matches real flags.
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        shape = RoundedCornerShape(4.dp),
        modifier = modifier.aspectRatio(1.5f),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = code,
                fontWeight = FontWeight.Bold,
                fontSize = fontSize,
                letterSpacing = 2.sp,
            )
        }
    }
}
