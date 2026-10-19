package com.aljaz.spendingtracker.automation.externalnotifications

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.aljaz.spendingtracker.automation.BankMessageProcessor
import com.aljaz.spendingtracker.automation.parser.BankMessageParser
import com.aljaz.spendingtracker.domain.model.TransactionSource
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Receives notifications posted by other apps and turns bank transaction
 * notifications (e.g. from NLB Pay) into transactions.
 *
 * Privacy: Android shows this service every notification on the phone.
 * Only apps that a registered [BankMessageParser] accepts get past the
 * first check; everything else is ignored without reading its text.
 * Notification texts are never logged.
 *
 * The user must grant access once in: Settings → Notification access.
 */
@AndroidEntryPoint
class ExternalNotificationListener : NotificationListenerService() {

    @Inject
    lateinit var processor: BankMessageProcessor

    /** Every registered parser (see AutomationModule); used as the whitelist. */
    @Inject
    lateinit var parsers: Set<@JvmSuppressWildcards BankMessageParser>

    /** Background work for saving; cancelled when Android destroys the service. */
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * Called when the system connects (after access is granted, after a
     * reboot, or when Android restarts the service). Catches up on bank
     * notifications still in the shade that arrived while we weren't
     * listening. Already-saved ones are skipped by the duplicate check.
     */
    override fun onListenerConnected() {
        super.onListenerConnected()
        val active = try {
            activeNotifications.orEmpty()
        } catch (e: SecurityException) {
            emptyArray() // access was revoked in the meantime
        }
        active.forEach { handle(it, catchingUp = true) }
    }

    /** Called every time any app posts or updates a notification. */
    override fun onNotificationPosted(sbn: StatusBarNotification) {
        handle(sbn, catchingUp = false)
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    /**
     * Passes one notification to [BankMessageProcessor] if it comes from
     * a whitelisted app and has text.
     *
     * @param catchingUp true for notifications found at connect time; these
     *        aren't added to the unrecognized list, otherwise the same
     *        non-transaction notification would be stored again on
     *        every reconnect
     */
    private fun handle(sbn: StatusBarNotification, catchingUp: Boolean) {
        val sender = sbn.packageName

        // Privacy: stop here for every app no parser knows.
        if (parsers.none { it.acceptsSender(sender) }) return

        val notification = sbn.notification

        // Group summaries are empty "headers" that bundle several notifications.
        if (notification.flags and Notification.FLAG_GROUP_SUMMARY != 0) return

        val text = textOf(notification) ?: return

        scope.launch {
            try {
                val result = processor.process(
                    sender = sender,
                    body = text,
                    source = TransactionSource.NOTIFICATION,
                    rememberUnrecognized = !catchingUp
                )
                // Only the result type, never the text: "Saved", "Duplicate", ...
                Log.d(TAG, "$sender: ${result.javaClass.simpleName}")
            } catch (e: Exception) {
                // Never crash on an unexpected notification; log only the error type.
                Log.e(TAG, "Failed to process notification: ${e.javaClass.simpleName}")
            }
        }
    }

    /**
     * The full notification text: the expanded text (EXTRA_BIG_TEXT) if the
     * app provides one, otherwise the normal one-line text. Null if empty.
     */
    private fun textOf(notification: Notification): String? {
        val extras = notification.extras
        val text = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)
            ?: extras.getCharSequence(Notification.EXTRA_TEXT)
        return text?.toString()?.takeIf { it.isNotBlank() }
    }

    private companion object {
        const val TAG = "ExternalNotifications"
    }
}