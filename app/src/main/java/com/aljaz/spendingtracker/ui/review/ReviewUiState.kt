package com.aljaz.spendingtracker.ui.review

import com.aljaz.spendingtracker.domain.model.Category
import com.aljaz.spendingtracker.domain.model.Transaction

/**
 * Everything the Review screen shows at one moment.
 *
 * @property transactions automatic transactions waiting for review, newest first
 * @property categoriesById all categories, keyed by id for quick lookup
 * @property isLoading true until the first data has arrived
 */
data class ReviewUiState(
    val transactions: List<Transaction> = emptyList(),
    val categoriesById: Map<Long, Category> = emptyMap(),
    val isLoading: Boolean = true
)