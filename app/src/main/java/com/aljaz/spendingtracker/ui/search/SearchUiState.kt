package com.aljaz.spendingtracker.ui.search

import com.aljaz.spendingtracker.domain.model.Category
import com.aljaz.spendingtracker.domain.model.Transaction
import com.aljaz.spendingtracker.domain.model.TransactionType

/**
 * Results and lookup data for the Search screen.
 * The filter itself is exposed separately by the ViewModel, so typing
 * in the search field always stays responsive.
 *
 * @property results matching transactions, newest first
 * @property categories all categories, for chips and row colors
 * @property isLoading true until the first results have arrived
 */
data class SearchUiState(
    val results: List<Transaction> = emptyList(),
    val categories: List<Category> = emptyList(),
    val isLoading: Boolean = true
) {
    /** Categories keyed by id, for drawing rows. */
    val categoriesById: Map<Long, Category>
        get() = categories.associateBy { it.id }

    /** Sum of the matching expenses, in minor units. */
    val expenseTotalMinor: Long
        get() = results.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amountMinor }

    /** Sum of the matching income, in minor units. */
    val incomeTotalMinor: Long
        get() = results.filter { it.type == TransactionType.INCOME }.sumOf { it.amountMinor }
}