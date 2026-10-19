package com.aljaz.spendingtracker.automation.externalnotifications

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationManagerCompat

/** Helpers for the "Notification access" special permission. */
object ExternalNotificationAccess {

    /** True if the user enabled notification access for this app. */
    fun isGranted(context: Context): Boolean =
        NotificationManagerCompat.getEnabledListenerPackages(context)
            .contains(context.packageName)

    /** Opens the system screen where the user can enable access. */
    fun openSettings(context: Context) {
        Log.d("NotifCapture", "openSettings called")
        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e("NotifCapture", "Could not open settings", e)
        }
    }
}