package com.aljaz.spendingtracker.ui.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aljaz.spendingtracker.automation.BankMessageProcessor
import com.aljaz.spendingtracker.automation.BankMessageResult
import com.aljaz.spendingtracker.automation.notifications.NotificationPreferences
import com.aljaz.spendingtracker.automation.parser.DhSmsParser
import com.aljaz.spendingtracker.core.AppResult
import com.aljaz.spendingtracker.core.MoneyFormatter
import com.aljaz.spendingtracker.domain.export.ImportSummary
import com.aljaz.spendingtracker.domain.model.TransactionSource
import com.aljaz.spendingtracker.domain.settings.PeriodSettingsRepository
import com.aljaz.spendingtracker.domain.usecase.ExportHistoryUseCase
import com.aljaz.spendingtracker.domain.usecase.ImportHistoryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/**
 * Logic behind the Settings screen: CSV export and import, time period,
 * notifications and the bank SMS test. (The Nextcloud backup has its own
 * screen and ViewModel.)
 *
 * Uses the application context (never an Activity) to open the files
 * the user picked, so the ViewModel can't leak a screen.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val exportHistory: ExportHistoryUseCase,
    private val importHistory: ImportHistoryUseCase,
    private val bankMessageProcessor: BankMessageProcessor,
    private val notificationPreferences: NotificationPreferences,
    private val periodSettings: PeriodSettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        _uiState.update { it.copy(notificationsEnabled = notificationPreferences.isEnabled()) }

        // Keep the shown period settings in sync with the stored ones.
        // Each collect runs forever, so each needs its own launch.
        viewModelScope.launch {
            periodSettings.monthStartDay.collect { day ->
                _uiState.update { it.copy(monthStartDay = day) }
            }
        }
        viewModelScope.launch {
            periodSettings.carryOver.collect { enabled ->
                _uiState.update { it.copy(carryOver = enabled) }
            }
        }
    }

    // ---------- Export ----------

    /** File name proposed in the "Save as" picker, e.g. "spending_2026-10-14.csv". */
    fun suggestedFileName(): String = "spending_${LocalDate.now()}.csv"

    /**
     * Called with the location the user picked in the "Save as" picker.
     *
     * @param uri the chosen file, or null if the user cancelled
     */
    fun onExportDestinationChosen(uri: Uri?) {
        if (uri == null) return // cancelled; nothing to do

        _uiState.update { it.copy(isExporting = true, message = null) }

        viewModelScope.launch(Dispatchers.IO) {
            val result = try {
                // "wt" = write and truncate, so overwriting an old file
                // never leaves leftover lines at the end.
                context.contentResolver.openOutputStream(uri, "wt")
                    ?.use { stream -> exportHistory(stream) }
                    ?: AppResult.Error("Could not open the chosen file.")
            } catch (e: Exception) {
                AppResult.Error("Export failed: ${e.message}")
            }

            val message = when (result) {
                is AppResult.Success -> "Exported ${result.data} transactions."
                is AppResult.Error -> result.message
            }
            _uiState.update { it.copy(isExporting = false, message = message) }
        }
    }

    // ---------- Import ----------

    /**
     * Called with the file the user picked for importing.
     *
     * @param uri the chosen file, or null if the user cancelled
     */
    fun onImportFileChosen(uri: Uri?) {
        if (uri == null) return // cancelled; nothing to do

        _uiState.update { it.copy(isImporting = true, message = null) }

        viewModelScope.launch(Dispatchers.IO) {
            val result = try {
                context.contentResolver.openInputStream(uri)
                    ?.use { stream -> importHistory(stream) }
                    ?: AppResult.Error("Could not open the chosen file.")
            } catch (e: Exception) {
                AppResult.Error("Import failed: ${e.message}")
            }

            val message = when (result) {
                is AppResult.Success -> describe(result.data)
                is AppResult.Error -> result.message
            }
            _uiState.update { it.copy(isImporting = false, message = message) }
        }
    }

    /** Builds the message shown after an import, listing at most 5 errors. */
    private fun describe(summary: ImportSummary): String = buildString {
        // Only mention what actually happened, e.g. "12 transactions imported, 3 already in the app."
        val parts = buildList {
            if (summary.transactionsImported > 0 || summary.duplicatesSkipped > 0) {
                add("${summary.transactionsImported} transactions imported")
            }
            if (summary.duplicatesSkipped > 0) add("${summary.duplicatesSkipped} already in the app")
            if (summary.categoriesAdded > 0) add("${summary.categoriesAdded} categories added")
            if (summary.categoriesRecolored > 0) add("${summary.categoriesRecolored} categories recolored")
            if (summary.categoriesCreated > 0) add("${summary.categoriesCreated} categories created")
            if (summary.rulesImported > 0) add("${summary.rulesImported} keyword rules imported")
        }
        append(
            if (parts.isEmpty()) {
                "Nothing new to import."
            } else {
                parts.joinToString(", ").replaceFirstChar { it.uppercase() } + "."
            }
        )

        if (summary.errors.isNotEmpty()) {
            append("\n\n${summary.errors.size} rows could not be read:")
            summary.errors.take(MAX_ERRORS_SHOWN).forEach { append("\n• $it") }
            val hidden = summary.errors.size - MAX_ERRORS_SHOWN
            if (hidden > 0) append("\n…and $hidden more.")
        }
    }

    // ---------- Time period ----------

    /** Saves the day of the month periods start on. Home updates immediately. */
    fun onMonthStartDayChange(day: Int) = periodSettings.setMonthStartDay(day)

    /** Turns carry-over on or off. Home updates immediately. */
    fun onCarryOverToggle(enabled: Boolean) = periodSettings.setCarryOver(enabled)

    // ---------- Notifications ----------

    /** Turns notifications about new bank transactions on or off. */
    fun onNotificationsToggle(enabled: Boolean) {
        notificationPreferences.setEnabled(enabled)
        _uiState.update { it.copy(notificationsEnabled = enabled) }
    }

    // ---------- Bank SMS ----------

    fun onSmsTestTextChange(value: String) =
        _uiState.update { it.copy(smsTest = it.smsTest.copy(text = value, result = null)) }

    /**
     * Runs the pasted text through exactly the same steps as a real SMS
     * from the bank: parse, categorize, save (or skip as duplicate).
     */
    fun onProcessSmsTestClick() {
        val test = _uiState.value.smsTest
        if (test.isProcessing || test.text.isBlank()) return

        _uiState.update { it.copy(smsTest = it.smsTest.copy(isProcessing = true, result = null)) }

        viewModelScope.launch {
            val result = bankMessageProcessor.process(
                sender = DhSmsParser.SENDER,
                body = test.text,
                source = TransactionSource.SMS
            )
            val message = when (result) {
                is BankMessageResult.Saved -> {
                    val t = result.transaction
                    "Saved: ${MoneyFormatter.format(t.amountMinor, t.currency)} at ${t.title}" +
                            " → ${result.categoryName ?: "Uncategorized (no rule matched)"}"
                }
                BankMessageResult.Duplicate -> "This transaction was already saved; skipped."
                BankMessageResult.Unrecognized -> "Not recognized as a DH transaction. Kept under Review → Unrecognized messages."
                BankMessageResult.NotFromBank -> "The sender is not a known bank."
            }
            _uiState.update {
                it.copy(smsTest = it.smsTest.copy(isProcessing = false, result = message))
            }
        }
    }

    private companion object {
        const val MAX_ERRORS_SHOWN = 5
    }
}