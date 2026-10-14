package com.aljaz.spendingtracker.ui.transactions

import com.aljaz.spendingtracker.domain.model.Category
import com.aljaz.spendingtracker.domain.model.TransactionType

/**
 * Everything the add/edit form shows at one moment.
 *
 * The screen only draws this state. It never changes it directly, only
 * through ViewModel functions. This keeps behavior predictable.
 *
 * @property isEditing true when an existing transaction is being edited
 * @property amountText what the user has typed in the amount field
 * @property categories all categories, of both types
 * @property selectedCategoryId the chosen category, or null for none
 * @property errorMessage a validation or save error to show, or null
 * @property isSaving true while a save or delete is running
 * @property showDeleteConfirm true while the "Delete?" dialog is open
 * @property isFinished becomes true after a successful save or delete,
 *           telling the screen to close itself
 */
data class AddTransactionUiState(
    val isEditing: Boolean = false,
    val type: TransactionType = TransactionType.EXPENSE,
    val amountText: String = "",
    val title: String = "",
    val note: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val categories: List<Category> = emptyList(),
    val selectedCategoryId: Long? = null,
    val errorMessage: String? = null,
    val isSaving: Boolean = false,
    val showDeleteConfirm: Boolean = false,
    val isFinished: Boolean = false
) {
    /** Only the categories that fit the currently chosen type. */
    val categoriesForType: List<Category>
        get() = categories.filter { it.type == type }
}