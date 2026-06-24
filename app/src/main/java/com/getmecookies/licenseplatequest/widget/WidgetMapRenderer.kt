package com.getmecookies.licenseplatequest.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.compose.ui.graphics.asAndroidPath
import com.getmecookies.licenseplatequest.ui.map.UsMapShapes
import kotlin.math.max
import kotlin.math.min

/**
 * Renders the lifetime/trip map to a plain [Bitmap] for the home-screen widget. Glance can't host
 * the in-app Compose `UsMap` Canvas, so we re-draw the bundled state shapes with `android.graphics`
 * and hand the result to Glance as an image. Reuses the already-parsed [UsMapShapes] geometry
 * (`StateShape.path` → `asAndroidPath()`), so there's no second SVG parse.
 *
 * Found states fill with [foundColor]; the rest with [baseColor]; all stroked with [outlineColor].
 * The viewBox is fit-and-centered into the target pixel size (same transform as the in-app map).
 */
object WidgetMapRenderer {

    fun render(
        shapes: UsMapShapes,
        foundCodes: Set<String>,
        widthPx: Int,
        heightPx: Int,
        foundColor: Int,
        baseColor: Int,
        outlineColor: Int,
    ): Bitmap {
        val w = max(1, widthPx)
        val h = max(1, heightPx)
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        if (shapes.width <= 0f || shapes.height <= 0f) return bitmap

        val canvas = Canvas(bitmap)
        val scale = min(w / shapes.width, h / shapes.height)
        val dx = (w - shapes.width * scale) / 2f
        val dy = (h - shapes.height * scale) / 2f

        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = outlineColor
            // ~1px after the canvas scale, so borders stay hairline at any widget size.
            strokeWidth = (1.1f / scale).coerceAtLeast(0.01f)
        }

        canvas.save()
        canvas.translate(dx, dy)
        canvas.scale(scale, scale)
        shapes.states.forEach { state ->
            val path = state.path.asAndroidPath()
            fill.color = if (state.code in foundCodes) foundColor else baseColor
            canvas.drawPath(path, fill)
            canvas.drawPath(path, stroke)
        }
        canvas.restore()
        return bitmap
    }
}
