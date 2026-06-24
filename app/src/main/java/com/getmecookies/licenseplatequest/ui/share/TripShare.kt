package com.getmecookies.licenseplatequest.ui.share

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.core.content.FileProvider
import com.getmecookies.licenseplatequest.domain.Analytics
import com.getmecookies.licenseplatequest.domain.NoOpAnalytics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * Saves [bitmap] as a PNG in the app cache and launches the system share sheet with it
 * (playtest note #4). The file lives under `cacheDir/shared/`, which the manifest FileProvider
 * exposes via a content URI so other apps can read it. Logs `share_completed` at dispatch — the
 * chooser is the OS's, so we can't observe which target (or whether) the user ultimately picks; the
 * event marks that the user invoked share and we handed off to the system sheet.
 */
suspend fun shareTripImage(
    context: Context,
    bitmap: Bitmap,
    chooserTitle: String,
    analytics: Analytics = NoOpAnalytics,
) {
    analytics.event("share_completed")
    val uri = withContext(Dispatchers.IO) {
        val dir = File(context.cacheDir, "shared").apply { mkdirs() }
        val file = File(dir, "trip_${System.currentTimeMillis()}.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(send, chooserTitle))
}
