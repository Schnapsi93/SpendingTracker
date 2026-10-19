package com.aljaz.spendingtracker.automation.externalnotifications

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat

/**
 * Helpers for the "Notification access" special permission.
 *
 * Unlike normal runtime permissions this one can't be requested with a
 * dialog; the user switches it on in system settings, so we only check
 * it and open the right settings screen.
 */
object ExternalNotificationAccess {

    /** True if the user enabled notification access for this app. */
    fun isGranted(context: Context): Boolean =
        NotificationManagerCompat.getEnabledListenerPackages(context)
            .contains(context.packageName)

    /** Opens the system screen where the user can enable access. */
    fun openSettings(context: Context) {
        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            // Some heavily customized systems hide this screen; fall back
            // to the general settings so the user can still find it.
            context.startActivity(
                Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }
}