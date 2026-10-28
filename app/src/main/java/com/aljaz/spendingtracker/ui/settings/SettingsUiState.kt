package com.aljaz.spendingtracker.ui.settings

/**
 * The "Test with a message" field in the Bank SMS card.
 *
 * @property text the pasted message
 * @property isProcessing true while it is being processed
 * @property result what happened, or null
 */
data class SmsTestState(
    val text: String = "",
    val isProcessing: Boolean = false,
    val result: String? = null
)

/**
 * Everything the Settings screen shows at one moment.
 *
 * @property isExporting true while an export is running
 * @property isImporting true while an import is running
 * @property message result of the last export or import, or null
 * @property smsTest the bank SMS test field
 * @property notificationsEnabled whether new bank transactions show a notification
 * @property monthStartDay the day of the month periods start on; 1 = calendar months
 * @property carryOver whether each period starts with the previous balance
 */
data class SettingsUiState(
    val isExporting: Boolean = false,
    val isImporting: Boolean = false,
    val message: String? = null,
    val smsTest: SmsTestState = SmsTestState(),
    val notificationsEnabled: Boolean = true,
    val monthStartDay: Int = 1,
    val carryOver: Boolean = false
) {
    /** True while any file operation runs; buttons are disabled meanwhile. */
    val isBusy: Boolean
        get() = isExporting || isImporting
}