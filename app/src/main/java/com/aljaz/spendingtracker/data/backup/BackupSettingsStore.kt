package com.aljaz.spendingtracker.data.backup

import android.content.Context
import com.aljaz.spendingtracker.domain.backup.BackupSettings
import com.aljaz.spendingtracker.domain.backup.BackupSettingsRepository
import com.aljaz.spendingtracker.domain.backup.CloudCredentials
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Keeps the backup settings in private app preferences.
 * The app password is stored only in encrypted form (see [SecretCipher]).
 */
@Singleton
class BackupSettingsStore @Inject constructor(
    @ApplicationContext context: Context,
    private val cipher: SecretCipher
) : BackupSettingsRepository {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override suspend fun load(): BackupSettings = withContext(Dispatchers.IO) {
        BackupSettings(
            serverUrl = prefs.getString(KEY_URL, "").orEmpty(),
            username = prefs.getString(KEY_USERNAME, "").orEmpty(),
            folder = prefs.getString(KEY_FOLDER, null) ?: BackupSettings.DEFAULT_FOLDER,
            hasPassword = prefs.contains(KEY_PASSWORD)
        )
    }

    override suspend fun save(settings: BackupSettings, newPassword: String?) =
        withContext(Dispatchers.IO) {
            prefs.edit().apply {
                putString(KEY_URL, settings.serverUrl)
                putString(KEY_USERNAME, settings.username)
                putString(KEY_FOLDER, settings.folder)
                if (newPassword != null) {
                    putString(KEY_PASSWORD, cipher.encrypt(newPassword))
                }
            }.apply()
        }

    override suspend fun loadCredentials(): CloudCredentials? = withContext(Dispatchers.IO) {
        val settings = load()
        val password = prefs.getString(KEY_PASSWORD, null)?.let { cipher.decrypt(it) }
        if (settings.serverUrl.isBlank() || settings.username.isBlank() || password == null) {
            null
        } else {
            CloudCredentials(settings.serverUrl, settings.username, password, settings.folder)
        }
    }

    private companion object {
        const val PREFS_NAME = "backup_settings"
        const val KEY_URL = "server_url"
        const val KEY_USERNAME = "username"
        const val KEY_FOLDER = "folder"
        const val KEY_PASSWORD = "password_encrypted"
    }
}