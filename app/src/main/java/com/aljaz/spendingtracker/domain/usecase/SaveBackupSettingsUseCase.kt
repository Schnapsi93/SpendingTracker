package com.aljaz.spendingtracker.domain.usecase

import com.aljaz.spendingtracker.core.AppResult
import com.aljaz.spendingtracker.domain.backup.BackupSettings
import com.aljaz.spendingtracker.domain.backup.BackupSettingsRepository
import java.net.URI
import java.net.URISyntaxException
import javax.inject.Inject

/** Validates and saves the Nextcloud settings from the form. */
class SaveBackupSettingsUseCase @Inject constructor(
    private val repository: BackupSettingsRepository
) {

    /**
     * @param newPassword the typed app password; empty keeps the stored one
     * @return [AppResult.Success] when saved, or [AppResult.Error] with a reason
     */
    suspend operator fun invoke(
        serverUrl: String,
        username: String,
        folder: String,
        newPassword: String
    ): AppResult<Unit> {
        val url = serverUrl.trim().trimEnd('/')
        if (!url.startsWith("https://", ignoreCase = true)) {
            return AppResult.Error(
                "The address must start with https:// so your password is sent encrypted."
            )
        }
        val host = try {
            URI(url).host
        } catch (e: URISyntaxException) {
            null
        }
        if (host.isNullOrBlank()) {
            return AppResult.Error("That doesn't look like a valid address.")
        }

        val user = username.trim()
        if (user.isEmpty()) {
            return AppResult.Error("Enter your Nextcloud username.")
        }

        val password = newPassword.trim().ifEmpty { null }
        if (password == null && !repository.load().hasPassword) {
            return AppResult.Error("Enter an app password.")
        }

        repository.save(
            BackupSettings(
                serverUrl = url,
                username = user,
                folder = folder.trim().trim('/').ifEmpty { BackupSettings.DEFAULT_FOLDER },
                hasPassword = true
            ),
            newPassword = password
        )
        return AppResult.Success(Unit)
    }
}