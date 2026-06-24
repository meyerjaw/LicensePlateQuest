package com.getmecookies.licenseplatequest.ui.share

import android.content.Context
import android.graphics.Bitmap
import androidx.test.core.app.ApplicationProvider
import com.getmecookies.licenseplatequest.domain.FakeAnalytics
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Verifies [shareTripImage] logs `share_completed` when a share is dispatched. The event is logged
 * before the OS hand-off (FileProvider URI + chooser), so the assertion isn't coupled to that
 * Android plumbing succeeding under Robolectric — any later exception is tolerated.
 */
@RunWith(RobolectricTestRunner::class)
class TripShareTest {

    @Test
    fun shareTripImage_logsShareCompleted() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
        val analytics = FakeAnalytics()

        // The chooser hand-off (startActivity / FileProvider) may throw under a unit-test context;
        // the analytics call happens first, which is what we're asserting.
        runCatching { shareTripImage(context, bitmap, "Share", analytics) }

        assertTrue(analytics.eventNames().contains("share_completed"))
    }
}
