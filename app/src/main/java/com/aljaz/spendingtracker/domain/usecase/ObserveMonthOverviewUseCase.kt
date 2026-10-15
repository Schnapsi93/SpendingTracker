package com.aljaz.spendingtracker.domain.usecase

import com.aljaz.spendingtracker.core.toEpochMillisRange
import com.aljaz.spendingtracker.domain.model.CategoryTotal
import com.aljaz.spendingtracker.domain.model.MonthOverview
import com.aljaz.spendingtracker.domain.model.MonthSummary
import com.aljaz.spendingtracker.domain.model.TransactionType
import com.aljaz.spendingtracker.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.YearMonth
import javax.inject.Inject

/**
 * Streams the transactions, totals and per-category spending for one
 * calendar month. Emits a new value every time the underlying data changes.
 */
class ObserveMonthOverviewUseCase @Inject constructor(
    private val repository: TransactionRepository
) {

    /**
     * Streams the transactions, totals and per-category spending for one
     * period (a calendar month, or e.g. 15th to 14th, see MonthRange.kt).
     * @param month the period to observe, named after the month it starts in
     * @param startDay the day of the month periods start on; 1 = calendar months
     */
    operator fun invoke(month: YearMonth, startDay: Int = 1): Flow<MonthOverview> {
        val (start, end) = month.toEpochMillisRange(startDay = startDay)

        return repository.observeBetween(start, end).map { transactions ->
            val expenses = transactions.filter { it.type == TransactionType.EXPENSE }
            val income = transactions
                .filter { it.type == TransactionType.INCOME }
                .sumOf { it.amountMinor }

            // Group expenses by category (null = uncategorized) and add them up.
            val expenseByCategory = expenses
                .groupBy { it.categoryId }
                .map { (categoryId, items) ->
                    CategoryTotal(categoryId, items.sumOf { it.amountMinor })
                }
                .sortedByDescending { it.totalMinor }

            MonthOverview(
                summary = MonthSummary(
                    totalIncomeMinor = income,
                    totalExpenseMinor = expenses.sumOf { it.amountMinor }
                ),
                transactions = transactions,
                expenseByCategory = expenseByCategory
            )
        }
    }
}