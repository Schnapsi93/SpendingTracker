package com.aljaz.spendingtracker.ui.transactions

import com.aljaz.spendingtracker.domain.model.Category
import com.aljaz.spendingtracker.domain.model.TransactionSource
import com.aljaz.spendingtracker.domain.model.TransactionType

/**
 * The "Apply to all future transactions" dialog.
 *
 * @property keyword the keyword being edited, pre-filled with the merchant
 * @property error why the keyword was rejected, or null
 */
data class RuleDialogState(
    val keyword: String,
    val error: String? = null
)

/**
 * Everything the add/edit form shows at one moment.
 *
 * The screen only draws this state. It never changes it directly, only
 * through ViewModel functions. This keeps behavior predictable.
 *
 * @property isEditing true when an existing transaction is being edited
 * @property source how the edited transaction entered the app (MANUAL for new ones)
 * @property amountText what the user has typed in the amount field
 * @property categories all categories, of both types
 * @property selectedCategoryId the chosen category, or null for none
 * @property errorMessage a validation or save error to show, or null
 * @property isSaving true while a save or delete is running
 * @property showDeleteConfirm true while the "Delete?" dialog is open
 * @property ruleDialog the open "Apply to future" dialog, or null
 * @property isFinished becomes true after a successful save or delete,
 *           telling the screen to close itself
 */
data class AddTransactionUiState(
    val isEditing: Boolean = false,
    val source: TransactionSource = TransactionSource.MANUAL,
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
    val ruleDialog: RuleDialogState? = null,
    val isFinished: Boolean = false
) {
    /** Only the categories that fit the currently chosen type. */
    val categoriesForType: List<Category>
        get() = categories.filter { it.type == type }

    /** Name of the selected category, or null if none is selected. */
    val selectedCategoryName: String?
        get() = categories.find { it.id == selectedCategoryId }?.name

    /**
     * Rules only make sense for automatic transactions (they have a merchant
     * name from the bank) and need a category to point to.
     */
    val canCreateRule: Boolean
        get() = isEditing &&
                (source == TransactionSource.SMS || source == TransactionSource.WALLET) &&
                selectedCategoryId != null
}