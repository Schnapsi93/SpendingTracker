package com.aljaz.spendingtracker.domain.usecase

import com.aljaz.spendingtracker.core.AppResult
import com.aljaz.spendingtracker.domain.backup.BackupSettingsRepository
import com.aljaz.spendingtracker.domain.backup.CloudStorage
import com.aljaz.spendingtracker.domain.export.HistoryExporter
import java.io.ByteArrayOutputStream
import java.io.IOException
import javax.inject.Inject

/**
 * Exports the complete history and uploads it to the cloud as
 * [BACKUP_FILE_NAME], replacing the previous backup.
 *
 * Used both by the "Back up now" button and the monthly background job.
 */
class RunCloudBackupUseCase @Inject constructor(
    private val settings: BackupSettingsRepository,
    private val exporter: HistoryExporter,
    private val storage: CloudStorage
) {

    /** @return [AppResult.Success] with the number of backed-up transactions */
    suspend operator fun invoke(): AppResult<Int> {
        val credentials = settings.loadCredentials()
            ?: return AppResult.Error(
                "Nextcloud backup is not set up, or the app password must be entered again."
            )

        // Build the CSV in memory; it's small, so this is fine.
        val buffer = ByteArrayOutputStream()
        val count = try {
            exporter.export(buffer)
        } catch (e: IOException) {
            return AppResult.Error("Could not create the backup: ${e.message}")
        }

        // Safety rule: never replace a real backup with an empty one.
        if (count == 0) {
            return AppResult.Error(
                "There are no transactions, so nothing was uploaded. " +
                        "An empty backup never replaces an existing one."
            )
        }

        return when (val upload = storage.upload(credentials, BACKUP_FILE_NAME, buffer.toByteArray())) {
            is AppResult.Error -> upload
            is AppResult.Success -> {
                settings.setLastBackupAt(System.currentTimeMillis())
                AppResult.Success(count)
            }
        }
    }

    companion object {
        /** The single backup file in the Nextcloud folder; each backup replaces it. */
        const val BACKUP_FILE_NAME = "spending_backup.csv"
    }
}