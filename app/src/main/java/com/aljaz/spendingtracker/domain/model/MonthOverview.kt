package com.aljaz.spendingtracker.domain.model

/**
 * Totals for one month.
 *
 * For now all amounts are assumed to be in a single currency.
 * Multi-currency support would change how these are calculated.
 */
data class MonthSummary(
    val totalIncomeMinor: Long = 0,
    val totalExpenseMinor: Long = 0
) {
    /** Income minus expenses. Negative when you spent more than you earned. */
    val balanceMinor: Long
        get() = totalIncomeMinor - totalExpenseMinor
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