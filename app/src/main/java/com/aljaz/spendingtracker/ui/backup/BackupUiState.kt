package com.aljaz.spendingtracker.ui.backup

import com.aljaz.spendingtracker.domain.backup.BackupSettings

/**
 * Everything the Backup account screen shows at one moment.
 *
 * @property password the newly typed app password; empty means
 *           "keep the one already saved"
 * @property hasSavedPassword true when an app password is stored
 * @property isWorking true while saving and testing
 * @property isBackingUp true while "Back up now" is running
 * @property lastBackupText when the last backup succeeded, formatted, or null if never
 * @property message result of the last test or backup, or null
 */
data class BackupUiState(
    val serverUrl: String = "",
    val username: String = "",
    val password: String = "",
    val folder: String = BackupSettings.DEFAULT_FOLDER,
    val hasSavedPassword: Boolean = false,
    val isWorking: Boolean = false,
    val isBackingUp: Boolean = false,
    val lastBackupText: String? = null,
    val message: String? = null
) {
    /** True while anything runs; the buttons are disabled meanwhile. */
    val isBusy: Boolean
        get() = isWorking || isBackingUp
}