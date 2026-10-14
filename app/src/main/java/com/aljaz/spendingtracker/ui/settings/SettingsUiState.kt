package com.aljaz.spendingtracker.ui.settings

import com.aljaz.spendingtracker.domain.backup.BackupSettings

/**
 * The Nextcloud form.
 *
 * @property password the newly typed app password; empty means
 *           "keep the one already saved"
 * @property hasSavedPassword true when an app password is stored
 * @property isWorking true while saving and testing
 * @property isBackingUp true while "Back up now" is running
 * @property lastBackupText when the last backup succeeded, formatted, or null if never
 * @property message result of the last test, or null
 */
data class CloudFormState(
    val serverUrl: String = "",
    val username: String = "",
    val password: String = "",
    val folder: String = BackupSettings.DEFAULT_FOLDER,
    val hasSavedPassword: Boolean = false,
    val isWorking: Boolean = false,
    val isBackingUp: Boolean = false,
    val lastBackupText: String? = null,
    val message: String? = null
)

/**
 * Everything the Settings screen shows at one moment.
 *
 * @property isExporting true while an export is running
 * @property isImporting true while an import is running
 * @property message result of the last export or import, or null
 * @property cloud the Nextcloud backup form
 */
data class SettingsUiState(
    val isExporting: Boolean = false,
    val isImporting: Boolean = false,
    val message: String? = null,
    val cloud: CloudFormState = CloudFormState()
) {
    /** True while any file operation runs; buttons are disabled meanwhile. */
    val isBusy: Boolean
        get() = isExporting || isImporting
}