package com.aljaz.spendingtracker.data.sharing

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.aljaz.spendingtracker.core.AppResult
import com.aljaz.spendingtracker.domain.sharing.SharedSyncRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

/**
 * Background job that runs one sync (see [SyncScheduler] for when).
 */
class SyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    /**
     * Android creates this worker, not Hilt, so it can't receive
     * dependencies through its constructor. This entry point lets it ask
     * Hilt for what it needs instead (same as BackupWorker).
     */
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface SyncWorkerEntryPoint {
        fun sharedSync(): SharedSyncRepository
    }

    override suspend fun doWork(): Result {
        val sync = EntryPointAccessors
            .fromApplication(applicationContext, SyncWorkerEntryPoint::class.java)
            .sharedSync()

        return when (sync.syncNow()) {
            is AppResult.Success -> Result.success()
            // Retry a few times with growing pauses; then wait for the next
            // scheduled run, so a lasting problem doesn't drain the battery.
            is AppResult.Error ->
                if (runAttemptCount < MAX_RETRIES) Result.retry() else Result.success()
        }
    }

    private companion object {
        const val MAX_RETRIES = 3
    }
}