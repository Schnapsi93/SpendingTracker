package com.aljaz.spendingtracker.data.sharing

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/** Registers the shared expense sync with Android's WorkManager. */
object SyncScheduler {

    private const val PERIODIC_WORK = "shared_sync_periodic"
    private const val ONE_TIME_WORK = "shared_sync_now"

    /** Only run when the phone is online; otherwise WorkManager waits. */
    private val onlineOnly = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    /**
     * Syncs every hour, mainly to download the partner's new expenses.
     * Safe to call on every app start: KEEP leaves an existing schedule alone.
     */
    fun schedulePeriodic(context: Context) {
        val request = PeriodicWorkRequestBuilder<SyncWorker>(1, TimeUnit.HOURS)
            .setConstraints(onlineOnly)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PERIODIC_WORK,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    /**
     * Syncs as soon as the phone is online.
     * APPEND_OR_REPLACE: if a sync is already running, another one follows
     * it, so changes made during a running sync are never left behind.
     */
    fun requestNow(context: Context) {
        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(onlineOnly)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            ONE_TIME_WORK,
            ExistingWorkPolicy.APPEND_OR_REPLACE,
            request
        )
    }

    /** Stops all syncing (on logout). */
    fun cancel(context: Context) {
        val workManager = WorkManager.getInstance(context)
        workManager.cancelUniqueWork(PERIODIC_WORK)
        workManager.cancelUniqueWork(ONE_TIME_WORK)
    }
}