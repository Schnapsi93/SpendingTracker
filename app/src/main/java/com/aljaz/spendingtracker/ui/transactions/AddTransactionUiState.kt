package com.aljaz.spendingtracker.ui.transactions

import com.aljaz.spendingtracker.domain.model.TransactionType

/**
 * Everything the Add Transaction screen shows at one moment.
 *
 * The screen only draws this state. It never changes it directly, only
 * through ViewModel functions. This keeps behavior predictable.
 *
 * @property amountText what the user has typed in the amount field
 * @property errorMessage a validation or save error to show, or null
 * @property isSaving true while the save is running
 * @property saved becomes true after a successful save, telling the
 *           screen to close itself
 */
data class AddTransactionUiState(
    val type: TransactionType = TransactionType.EXPENSE,
    val amountText: String = "",
    val title: String = "",
    val note: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val errorMessage: String? = null,
    val isSaving: Boolean = false,
    val saved: Boolean = false
)