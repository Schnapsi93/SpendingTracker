package com.aljaz.spendingtracker.data.backup

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.aljaz.spendingtracker.domain.usecase.RunScheduledBackupUseCase
import com.aljaz.spendingtracker.domain.usecase.ScheduledBackupResult
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

/**
 * Background job, started by Android about once a day (see [BackupScheduler]).
 * It only uploads when a backup is due this month.
 */
class BackupWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    /**
     * Android creates this worker, not Hilt, so it can't receive
     * dependencies through its constructor. This entry point lets it ask
     * Hilt for what it needs instead.
     */
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface BackupWorkerEntryPoint {
        fun runScheduledBackup(): RunScheduledBackupUseCase
    }

    override suspend fun doWork(): Result {
        val runScheduledBackup = EntryPointAccessors
            .fromApplication(applicationContext, BackupWorkerEntryPoint::class.java)
            .runScheduledBackup()

        return when (runScheduledBackup()) {
            ScheduledBackupResult.NotConfigured,
            ScheduledBackupResult.AlreadyDoneThisMonth,
            is ScheduledBackupResult.Completed -> Result.success()

            // Retry a few times with growing pauses; after that, give up
            // until tomorrow's run so a lasting problem doesn't drain the battery.
            is ScheduledBackupResult.Failed ->
                if (runAttemptCount < MAX_RETRIES) Result.retry() else Result.success()
        }
    }

    private companion object {
        const val MAX_RETRIES = 3
    }
}