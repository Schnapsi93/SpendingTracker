package com.aljaz.spendingtracker.ui.home

import com.aljaz.spendingtracker.domain.model.Category
import com.aljaz.spendingtracker.domain.model.CategoryTotal
import com.aljaz.spendingtracker.domain.model.MonthSummary
import com.aljaz.spendingtracker.domain.model.Transaction
import java.time.YearMonth

/**
 * Everything the Home screen shows at one moment.
 *
 * @property month the month currently displayed
 * @property summary income, expense and balance totals for that month
 * @property expenseByCategory expense totals per category, largest first
 * @property transactions the month's transactions, newest first
 * @property categoriesById all categories, keyed by id for quick lookup
 * @property unreviewedCount automatic transactions waiting for review, in all months
 * @property unrecognizedCount bank messages that couldn't be read
 * @property monthStartDay the day periods start on; 1 = calendar months
 * @property currentPeriod the period containing today, for the picker's "Today"
 */
data class HomeUiState(
    val month: YearMonth,
    val summary: MonthSummary = MonthSummary(),
    val expenseByCategory: List<CategoryTotal> = emptyList(),
    val transactions: List<Transaction> = emptyList(),
    val categoriesById: Map<Long, Category> = emptyMap(),
    val unreviewedCount: Int = 0,
    val unrecognizedCount: Int = 0,
    val monthStartDay: Int = 1,
    val currentPeriod: YearMonth = month
)