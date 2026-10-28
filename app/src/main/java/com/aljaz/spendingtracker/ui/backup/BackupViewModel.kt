package com.aljaz.spendingtracker.ui.backup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aljaz.spendingtracker.core.AppResult
import com.aljaz.spendingtracker.domain.backup.BackupSettingsRepository
import com.aljaz.spendingtracker.domain.usecase.RunCloudBackupUseCase
import com.aljaz.spendingtracker.domain.usecase.SaveBackupSettingsUseCase
import com.aljaz.spendingtracker.domain.usecase.TestCloudConnectionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import javax.inject.Inject

/**
 * Logic behind the Backup account screen: the Nextcloud settings form,
 * testing the connection, and running a backup on demand.
 */
@HiltViewModel
class BackupViewModel @Inject constructor(
    private val backupSettings: BackupSettingsRepository,
    private val saveBackupSettings: SaveBackupSettingsUseCase,
    private val testCloudConnection: TestCloudConnectionUseCase,
    private val runCloudBackup: RunCloudBackupUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(BackupUiState())
    val uiState: StateFlow<BackupUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch { refreshForm() }
    }

    fun onUrlChange(value: String) =
        _uiState.update { it.copy(serverUrl = value, message = null) }

    fun onUsernameChange(value: String) =
        _uiState.update { it.copy(username = value, message = null) }

    fun onPasswordChange(value: String) =
        _uiState.update { it.copy(password = value, message = null) }

    fun onFolderChange(value: String) =
        _uiState.update { it.copy(folder = value, message = null) }

    /** Saves the form; if that works, tests the connection with the saved settings. */
    fun onSaveAndTestClick() {
        val form = _uiState.value
        if (form.isBusy) return // ignore double taps

        _uiState.update { it.copy(isWorking = true, message = null) }

        viewModelScope.launch {
            val saved = saveBackupSettings(
                serverUrl = form.serverUrl,
                username = form.username,
                folder = form.folder,
                newPassword = form.password
            )
            if (saved is AppResult.Error) {
                // Keep what the user typed so they can correct it.
                _uiState.update { it.copy(isWorking = false, message = saved.message) }
                return@launch
            }

            val message = when (val test = testCloudConnection()) {
                is AppResult.Success -> "Connected. The backup folder is ready."
                is AppResult.Error -> "Saved, but the connection failed: ${test.message}"
            }
            refreshForm() // shows the cleaned-up values and clears the password field
            _uiState.update { it.copy(isWorking = false, message = message) }
        }
    }

    /** Uploads a backup right away, regardless of the monthly schedule. */
    fun onBackupNowClick() {
        if (_uiState.value.isBusy) return // ignore double taps

        _uiState.update { it.copy(isBackingUp = true, message = null) }

        viewModelScope.launch {
            val message = when (val result = runCloudBackup()) {
                is AppResult.Success ->
                    "Backed up ${result.data} transactions to ${RunCloudBackupUseCase.BACKUP_FILE_NAME}."
                is AppResult.Error -> "Backup failed: ${result.message}"
            }
            refreshForm() // updates "Last backup"
            _uiState.update { it.copy(isBackingUp = false, message = message) }
        }
    }

    /** Loads the saved settings into the form. The password field always starts empty. */
    private suspend fun refreshForm() {
        val settings = backupSettings.load()
        val lastBackup = backupSettings.lastBackupAt()?.let { formatTime(it) }
        _uiState.update {
            it.copy(
                serverUrl = settings.serverUrl,
                username = settings.username,
                folder = settings.folder,
                password = "",
                hasSavedPassword = settings.hasPassword,
                lastBackupText = lastBackup
            )
        }
    }

    /** e.g. "Oct 14, 2026, 22:15", following the phone's language. */
    private fun formatTime(epochMillis: Long): String =
        Instant.ofEpochMilli(epochMillis)
            .atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT))
}