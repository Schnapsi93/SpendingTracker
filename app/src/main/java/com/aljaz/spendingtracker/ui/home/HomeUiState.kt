package com.aljaz.spendingtracker.ui.home

import com.aljaz.spendingtracker.domain.model.MonthSummary
import com.aljaz.spendingtracker.domain.model.Transaction
import java.time.YearMonth

/**
 * Everything the Home screen shows at one moment.
 *
 * @property month the month currently displayed
 * @property summary income, expense and balance totals for that month
 * @property transactions the month's transactions, newest first
 */
data class HomeUiState(
    val month: YearMonth,
    val summary: MonthSummary = MonthSummary(),
    val transactions: List<Transaction> = emptyList()
)