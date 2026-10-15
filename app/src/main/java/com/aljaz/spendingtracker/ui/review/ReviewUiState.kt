package com.aljaz.spendingtracker.ui.review

import com.aljaz.spendingtracker.domain.model.Category
import com.aljaz.spendingtracker.domain.model.Transaction
import com.aljaz.spendingtracker.domain.model.UnrecognizedMessage

/**
 * Everything the Review screen shows at one moment.
 *
 * @property transactions automatic transactions waiting for review, newest first
 * @property unrecognized bank messages that couldn't be read, newest first
 * @property categoriesById all categories, keyed by id for quick lookup
 * @property notice result of the last "Try again", or null
 * @property isLoading true until the first data has arrived
 */
data class ReviewUiState(
    val transactions: List<Transaction> = emptyList(),
    val unrecognized: List<UnrecognizedMessage> = emptyList(),
    val categoriesById: Map<Long, Category> = emptyMap(),
    val notice: String? = null,
    val isLoading: Boolean = true
) {
    /** True when there is nothing at all to look at. */
    val isEmpty: Boolean
        get() = transactions.isEmpty() && unrecognized.isEmpty()
}