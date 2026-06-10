package com.getmecookies.licenseplatequest.ui.components

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.getmecookies.licenseplatequest.LicensePlateQuestApp
import com.getmecookies.licenseplatequest.R
import com.getmecookies.licenseplatequest.notifications.NOTIFICATION_PRIMER_SNOOZE
import com.getmecookies.licenseplatequest.notifications.NotificationPrimerAction
import com.getmecookies.licenseplatequest.notifications.notificationPrimerAction
import com.getmecookies.licenseplatequest.notifications.resolveSnooze

/** Imperative handle returned by [rememberNotificationPermissionPrimer]. */
class NotificationPermissionPrimer internal constructor(
    private val onRequest: (force: Boolean, onResult: (granted: Boolean) -> Unit) -> Unit,
) {
    /**
     * Trigger the flow. [force] = an explicit "enable reminders" action, which ignores the
     * post-"Not now" snooze. [onResult] reports whether notifications are usable afterward —
     * true when already/now granted, false on decline, dismiss, or the settings deep-link — so a
     * caller (e.g. the Settings toggle) can reflect the real outcome. No-ops on older Android.
     */
    fun request(force: Boolean = false, onResult: (granted: Boolean) -> Unit = {}) =
        onRequest(force, onResult)
}

private enum class PrimerDialog { None, Primer, Settings }

/**
 * Pre-permission priming for `POST_NOTIFICATIONS`: shows a friendly in-app rationale before the
 * system dialog (and a "turn it on in Settings" deep-link once permanently denied), so the family
 * understands the one overdue-trip nudge before Android asks. Reusable — drop it into any screen
 * that's about to do something notification-gated. Renders its own dialogs; just call [request].
 */
@Composable
fun rememberNotificationPermissionPrimer(): NotificationPermissionPrimer {
    val context = LocalContext.current
    // Null when the host app isn't LicensePlateQuestApp (e.g. an isolated Compose UI test); the
    // primer then no-ops gracefully instead of crashing the composition.
    val prefs = remember(context) {
        (context.applicationContext as? LicensePlateQuestApp)?.container?.uiPreferences
    }
    val activity = remember(context) { context.findActivity() }

    var dialog by remember { mutableStateOf(PrimerDialog.None) }
    // The pending caller callback, carried across the dialog interaction / system prompt.
    var pendingResult by remember { mutableStateOf<((Boolean) -> Unit)?>(null) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { isGranted ->
        prefs?.notificationRequested = true
        pendingResult?.invoke(isGranted)
        pendingResult = null
    }

    fun snooze() {
        prefs?.notificationPrimerSnooze = NOTIFICATION_PRIMER_SNOOZE
    }

    /** User backed out of the primer/settings dialog — snooze and report "not granted". */
    fun decline() {
        dialog = PrimerDialog.None
        snooze()
        pendingResult?.invoke(false)
        pendingResult = null
    }

    fun trigger(force: Boolean, onResult: (Boolean) -> Unit) {
        val needsPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
        val granted = !needsPermission || ContextCompat.checkSelfPermission(
            context, Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED

        if (granted) {
            onResult(true)
            return
        }
        // Without prefs (non-app context, e.g. a test) we can't track snooze/asked — just bail.
        if (prefs == null) {
            onResult(false)
            return
        }

        val snoozeDecision = resolveSnooze(force, prefs.notificationPrimerSnooze)
        if (snoozeDecision.nextSnooze != prefs.notificationPrimerSnooze) {
            prefs.notificationPrimerSnooze = snoozeDecision.nextSnooze
        }
        if (snoozeDecision.skip) {
            onResult(false)
            return
        }

        val rationale = activity?.let {
            ActivityCompat.shouldShowRequestPermissionRationale(
                it,
                Manifest.permission.POST_NOTIFICATIONS
            )
        } ?: false

        when (notificationPrimerAction(
            needsPermission,
            granted,
            prefs.notificationRequested,
            rationale
        )) {
            NotificationPrimerAction.NONE -> onResult(true)
            NotificationPrimerAction.SHOW_PRIMER -> {
                pendingResult = onResult
                dialog = PrimerDialog.Primer
            }

            NotificationPrimerAction.SHOW_SETTINGS -> {
                pendingResult = onResult
                dialog = PrimerDialog.Settings
            }
        }
    }

    when (dialog) {
        PrimerDialog.None -> Unit

        PrimerDialog.Primer -> AlertDialog(
            onDismissRequest = { decline() },
            icon = {
                Image(
                    painter = painterResource(R.drawable.ic_reminder_bell),
                    contentDescription = null,
                    modifier = Modifier.size(56.dp),
                )
            },
            title = { Text(stringResource(R.string.notif_primer_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.notif_primer_body))
                    PrimerBenefit(stringResource(R.string.notif_primer_point_one))
                    PrimerBenefit(stringResource(R.string.notif_primer_point_two))
                    PrimerBenefit(stringResource(R.string.notif_primer_point_three))
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    dialog = PrimerDialog.None
                    launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }) { Text(stringResource(R.string.notif_primer_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { decline() }) {
                    Text(stringResource(R.string.notif_primer_dismiss))
                }
            },
        )

        PrimerDialog.Settings -> AlertDialog(
            onDismissRequest = { decline() },
            icon = { Icon(Icons.Filled.Settings, contentDescription = null) },
            title = { Text(stringResource(R.string.notif_primer_settings_title)) },
            text = { Text(stringResource(R.string.notif_primer_settings_body)) },
            confirmButton = {
                TextButton(onClick = {
                    dialog = PrimerDialog.None
                    context.openAppNotificationSettings()
                    // We can't know if they'll flip it on in Settings; report not-granted for now.
                    pendingResult?.invoke(false)
                    pendingResult = null
                }) { Text(stringResource(R.string.notif_primer_settings_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { decline() }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }

    return NotificationPermissionPrimer(onRequest = ::trigger)
}

/** A single benefit line in the primer: a small accent check beside the [text]. */
@Composable
private fun PrimerBenefit(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            Icons.Filled.Check,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp),
        )
        Text(text = text, style = MaterialTheme.typography.bodyMedium)
    }
}

/** Open this app's system notification settings (used when the permission is permanently denied). */
private fun Context.openAppNotificationSettings() {
    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
        putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    startActivity(intent)
}

/** Walk up the Context wrappers to the hosting Activity, if any. */
private fun Context.findActivity(): Activity? {
    var ctx: Context? = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}
