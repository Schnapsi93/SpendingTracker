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

/** Everything the Home screen needs for one month. */
data class MonthOverview(
    val summary: MonthSummary,
    val transactions: List<Transaction>
)