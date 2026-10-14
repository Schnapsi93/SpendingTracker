package com.aljaz.spendingtracker.domain.usecase

import com.aljaz.spendingtracker.core.AppResult
import com.aljaz.spendingtracker.domain.backup.BackupSettingsRepository
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import javax.inject.Inject

/** What the daily background check decided. */
sealed interface ScheduledBackupResult {
    /** No Nextcloud settings yet; nothing to do. */
    data object NotConfigured : ScheduledBackupResult

    /** A backup already succeeded this calendar month. */
    data object AlreadyDoneThisMonth : ScheduledBackupResult

    /** A backup was made now. */
    data class Completed(val transactionCount: Int) : ScheduledBackupResult

    /** A backup was due but failed; the job will try again later. */
    data class Failed(val message: String) : ScheduledBackupResult
}

/**
 * Runs a backup only if none has succeeded yet in the current calendar
 * month. Called once a day by the background job, so a backup happens
 * on the 1st, or on the first day after that when the phone is online.
 */
class RunScheduledBackupUseCase @Inject constructor(
    private val settings: BackupSettingsRepository,
    private val runCloudBackup: RunCloudBackupUseCase
) {

    /** @param zone decides when a month begins (the phone's time zone by default) */
    suspend operator fun invoke(zone: ZoneId = ZoneId.systemDefault()): ScheduledBackupResult {
        if (settings.loadCredentials() == null) {
            return ScheduledBackupResult.NotConfigured
        }

        val lastBackup = settings.lastBackupAt()
        if (lastBackup != null) {
            val lastBackupMonth = YearMonth.from(Instant.ofEpochMilli(lastBackup).atZone(zone))
            if (lastBackupMonth == YearMonth.now(zone)) {
                return ScheduledBackupResult.AlreadyDoneThisMonth
            }
        }

        return when (val result = runCloudBackup()) {
            is AppResult.Success -> ScheduledBackupResult.Completed(result.data)
            is AppResult.Error -> ScheduledBackupResult.Failed(result.message)
        }
    }
}