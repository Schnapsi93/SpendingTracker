package com.aljaz.spendingtracker.domain.usecase

import com.aljaz.spendingtracker.core.AppResult
import com.aljaz.spendingtracker.domain.backup.BackupSettingsRepository
import com.aljaz.spendingtracker.domain.backup.CloudStorage
import javax.inject.Inject

/** Checks the saved settings by logging in and preparing the backup folder. */
class TestCloudConnectionUseCase @Inject constructor(
    private val settings: BackupSettingsRepository,
    private val storage: CloudStorage
) {

    suspend operator fun invoke(): AppResult<Unit> {
        val credentials = settings.loadCredentials()
            ?: return AppResult.Error(
                "Settings are incomplete, or the saved password can't be read. " +
                        "Enter the app password again."
            )
        return storage.testConnection(credentials)
    }
}