package com.aljaz.spendingtracker.domain.model

import com.aljaz.spendingtracker.core.MoneyParser

/** Which categories a search includes. */
sealed interface CategoryFilter {
    /** No category restriction. */
    data object AllCategories : CategoryFilter

    /** Only transactions without a category. */
    data object Uncategorized : CategoryFilter

    /** Only transactions in one category. */
    data class Specific(val categoryId: Long) : CategoryFilter
}

/**
 * Search criteria for transactions. Every criterion that is set must match.
 *
 * @property query text to find in the title or note (case-insensitive),
 *           or an exact amount such as "161,77"
 * @property type only expenses or only income; null for both
 * @property category which categories to include
 */
data class TransactionFilter(
    val query: String = "",
    val type: TransactionType? = null,
    val category: CategoryFilter = CategoryFilter.AllCategories
) {
    /** True when nothing restricts the results. */
    val isEmpty: Boolean
        get() = query.isBlank() && type == null && category == CategoryFilter.AllCategories

    /** @return true if [transaction] meets every criterion */
    fun matches(transaction: Transaction): Boolean {
        if (type != null && transaction.type != type) return false

        when (category) {
            CategoryFilter.AllCategories -> Unit
            CategoryFilter.Uncategorized -> if (transaction.categoryId != null) return false
            is CategoryFilter.Specific -> if (transaction.categoryId != category.categoryId) return false
        }

        val text = query.trim()
        if (text.isEmpty()) return true

        // lowercase() handles č, š, ž correctly, unlike SQLite's LIKE.
        val needle = text.lowercase()
        if (transaction.title.lowercase().contains(needle)) return true
        if (transaction.note?.lowercase()?.contains(needle) == true) return true

        // "161,77" or "161.77" also finds that exact amount.
        val amount = MoneyParser.parseToMinor(text)
        return amount != null && amount == transaction.amountMinor
    }
}