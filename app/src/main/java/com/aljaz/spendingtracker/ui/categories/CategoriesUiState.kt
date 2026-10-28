package com.aljaz.spendingtracker.ui.categories

import com.aljaz.spendingtracker.domain.model.Category
import com.aljaz.spendingtracker.domain.model.TransactionType

/**
 * What the add/edit category dialog currently shows.
 *
 * @property existing the category being edited, or null when creating a new one
 * @property errorMessage a validation error to show under the name, or null
 * @property isSplit state of the "Split with partner" switch
 * @property splitPercentText the payer's default share as typed, e.g. "50"
 * @property splitErrorMessage a validation error to show under the share, or null
 */
data class CategoryEditorState(
    val existing: Category? = null,
    val name: String = "",
    val type: TransactionType = TransactionType.EXPENSE,
    val colorArgb: Int = CategoryPalette.colors.first(),
    val errorMessage: String? = null,
    val isSplit: Boolean = false,
    val splitPercentText: String = "50",
    val splitErrorMessage: String? = null
) {
    /** True when creating a new category rather than editing one. */
    val isNew: Boolean
        get() = existing == null

    /** Splitting is only offered for expense categories. */
    val canSplit: Boolean
        get() = type == TransactionType.EXPENSE
}

/**
 * Everything the Categories screen shows at one moment.
 *
 * @property editor the open add/edit dialog, or null when it is closed
 * @property pendingDelete the category waiting for delete confirmation, or null
 */
data class CategoriesUiState(
    val expenseCategories: List<Category> = emptyList(),
    val incomeCategories: List<Category> = emptyList(),
    val editor: CategoryEditorState? = null,
    val pendingDelete: Category? = null
)