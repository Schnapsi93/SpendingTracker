package com.aljaz.spendingtracker.domain.backup

/** Stores the Nextcloud settings and the (encrypted) app password. */
interface BackupSettingsRepository {

    /** The current settings; empty values if nothing was saved yet. */
    suspend fun load(): BackupSettings

    /**
     * Saves the settings.
     *
     * @param newPassword a new app password, or null to keep the stored one
     */
    suspend fun save(settings: BackupSettings, newPassword: String?)

    /**
     * @return everything needed to connect, or null if the settings are
     *         incomplete or the stored password can't be decrypted
     */
    suspend fun loadCredentials(): CloudCredentials?
}