package com.aljaz.spendingtracker.automation.externalnotifications

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

/**
 * Receives notifications posted by other apps.
 * STEP 1 – DEBUG: only logs them, so we learn NLB Pay's package name
 * and which field holds the full text. Replaced by a whitelist in step 3.
 */
class ExternalNotificationListener : NotificationListenerService() {

    /** Called when the system connects (after access is granted or on restart). */
    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d(TAG, "Listener connected")
        // Notifications already sitting in the shade:
        activeNotifications?.forEach { logNotification(it, "ACTIVE") }
    }

    /** Called every time any app posts or updates a notification. */
    override fun onNotificationPosted(sbn: StatusBarNotification) {
        logNotification(sbn, "POSTED")
    }

    /** Writes package name and text fields to Logcat. */
    private fun logNotification(sbn: StatusBarNotification, prefix: String) {
        val extras = sbn.notification.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)
        Log.d(TAG, "$prefix pkg=${sbn.packageName} title=$title | text=$text | bigText=$bigText")
    }

    companion object {
        private const val TAG = "NotifCapture"
    }
}