package com.aljaz.spendingtracker.domain.backup

/**
 * Nextcloud backup settings as shown in the settings form.
 *
 * The password itself is deliberately not part of this class, so it
 * can't end up on screen or in logs by accident.
 *
 * @property serverUrl e.g. "https://cloud.example.com", without a trailing slash
 * @property folder folder in Nextcloud where backups are stored
 * @property hasPassword true when an app password is stored
 */
data class BackupSettings(
    val serverUrl: String = "",
    val username: String = "",
    val folder: String = DEFAULT_FOLDER,
    val hasPassword: Boolean = false
) {
    companion object {
        const val DEFAULT_FOLDER = "myapps/spender/backups"
    }
}

/**
 * Everything needed to talk to the server, including the password.
 * Only created right before a connection is made.
 */
data class CloudCredentials(
    val serverUrl: String,
    val username: String,
    val password: String,
    val folder: String
) {
    /** Never prints the password, e.g. in logs or crash reports. */
    override fun toString(): String =
        "CloudCredentials(serverUrl=$serverUrl, username=$username, folder=$folder, password=***)"
}