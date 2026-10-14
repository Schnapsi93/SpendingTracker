package com.aljaz.spendingtracker.automation.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.aljaz.spendingtracker.automation.BankMessageProcessor
import com.aljaz.spendingtracker.domain.model.TransactionSource
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Called by Android for every incoming SMS (registered in AndroidManifest.xml).
 *
 * It hands each message to [BankMessageProcessor], which ignores everything
 * that isn't from a known bank. Message texts are never logged.
 */
class SmsReceiver : BroadcastReceiver() {

    /** Lets this receiver ask Hilt for the processor (Android creates it, not Hilt). */
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface SmsReceiverEntryPoint {
        fun bankMessageProcessor(): BankMessageProcessor
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isNullOrEmpty()) return

        val processor = EntryPointAccessors
            .fromApplication(context.applicationContext, SmsReceiverEntryPoint::class.java)
            .bankMessageProcessor()

        // Keeps the receiver alive while saving (Android allows a few seconds),
        // then tells Android it's done.
        val pendingResult = goAsync()

        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                // A long SMS arrives in several parts; join the parts per sender.
                messages
                    .groupBy { it.originatingAddress.orEmpty() }
                    .forEach { (sender, parts) ->
                        val body = parts.joinToString("") { it.messageBody.orEmpty() }
                        processor.process(sender, body, TransactionSource.SMS)
                    }
            } catch (e: Exception) {
                // Never crash on an unexpected message; log only the error type.
                Log.e(TAG, "Failed to process SMS: ${e.javaClass.simpleName}")
            } finally {
                pendingResult.finish()
            }
        }
    }

    private companion object {
        const val TAG = "SmsReceiver"
    }
}