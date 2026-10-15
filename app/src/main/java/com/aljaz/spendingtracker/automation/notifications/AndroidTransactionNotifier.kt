package com.aljaz.spendingtracker.automation.notifications

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.aljaz.spendingtracker.MainActivity
import com.aljaz.spendingtracker.R
import com.aljaz.spendingtracker.core.MoneyFormatter
import com.aljaz.spendingtracker.domain.model.Transaction
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [TransactionNotifier] that shows Android notifications. Tapping one
 * opens the app on the Review screen.
 *
 * Does nothing if the user turned notifications off in Settings, or
 * hasn't granted the permission.
 */
@Singleton
class AndroidTransactionNotifier @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferences: NotificationPreferences
) : TransactionNotifier {

    override fun notifySaved(transaction: Transaction, categoryName: String?) {
        val amount = MoneyFormatter.format(transaction.amountMinor, transaction.currency)
        val detail = if (categoryName != null) {
            "→ $categoryName. Tap to review."
        } else {
            "Uncategorized. Tap to choose a category."
        }
        // One notification per transaction, so several purchases don't replace each other.
        show(id = transaction.id.toInt(), title = "$amount · ${transaction.title}", text = detail)
    }

    override fun notifyUnrecognized() {
        show(
            id = UNRECOGNIZED_ID,
            title = "A bank message couldn't be read",
            text = "Tap to see it in the review list."
        )
    }

    /** Shows one notification, if the user allows it. */
    @SuppressLint("MissingPermission") // checked right below
    private fun show(id: Int, title: String, text: String) {
        if (!preferences.isEnabled()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return // Android 13+ without permission: stay silent
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(openReviewIntent())
            .setAutoCancel(true) // disappears when tapped
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        NotificationManagerCompat.from(context).notify(id, notification)
    }

    /** Opens MainActivity with a flag that tells it to show the Review screen. */
    private fun openReviewIntent(): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra(MainActivity.EXTRA_OPEN_REVIEW, true)
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        /** The channel users see under the app's notification settings. */
        const val CHANNEL_ID = "new_transactions"

        /** Fixed id: several unreadable messages share one notification. */
        private const val UNRECOGNIZED_ID = -1

        /**
         * Registers the notification channel. Safe to call on every app start;
         * Android ignores it if the channel already exists.
         */
        fun createChannel(context: Context) {
            val channel = NotificationChannelCompat.Builder(
                CHANNEL_ID,
                NotificationManagerCompat.IMPORTANCE_DEFAULT
            )
                .setName("New transactions")
                .setDescription("A transaction was added from a bank message")
                .build()
            NotificationManagerCompat.from(context).createNotificationChannel(channel)
        }
    }
}