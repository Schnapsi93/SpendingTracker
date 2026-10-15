package com.aljaz.spendingtracker.domain.model

/**
 * Totals for one period.
 *
 * For now all amounts are assumed to be in a single currency.
 * Multi-currency support would change how these are calculated.
 *
 * @property carriedOverMinor balance left over from all earlier periods;
 *           0 when carry-over is off. May be negative.
 */
data class MonthSummary(
    val totalIncomeMinor: Long = 0,
    val totalExpenseMinor: Long = 0,
    val carriedOverMinor: Long = 0
) {
    /** Carried-over amount plus income minus expenses. Negative when more was spent. */
    val balanceMinor: Long
        get() = carriedOverMinor + totalIncomeMinor - totalExpenseMinor
}

/**
 * How much was spent in one category during a month.
 *
 * @property categoryId the category, or null for uncategorized transactions
 * @property totalMinor sum of the expenses, in minor units
 */
data class CategoryTotal(
    val categoryId: Long?,
    val totalMinor: Long
)

/**
 * Everything the Home screen needs for one month.
 *
 * @property expenseByCategory expense totals per category, largest first
 */
data class MonthOverview(
    val summary: MonthSummary,
    val transactions: List<Transaction>,
    val expenseByCategory: List<CategoryTotal>
)