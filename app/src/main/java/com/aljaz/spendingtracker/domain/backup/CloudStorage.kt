package com.aljaz.spendingtracker.domain.backup

import com.aljaz.spendingtracker.core.AppResult

/**
 * A remote place where backups can be stored. Nextcloud today; another
 * service would only need a new implementation of this interface.
 */
interface CloudStorage {

    /** Logs in and makes sure the backup folder exists. */
    suspend fun testConnection(credentials: CloudCredentials): AppResult<Unit>

    /** Uploads [content] as [fileName] into the backup folder, replacing any existing file. */
    suspend fun upload(
        credentials: CloudCredentials,
        fileName: String,
        content: ByteArray
    ): AppResult<Unit>
}