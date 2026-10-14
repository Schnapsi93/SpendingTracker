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

    /** @param month the month to observe, e.g. YearMonth.now() */
    operator fun invoke(month: YearMonth): Flow<MonthOverview> {
        val (start, end) = month.toEpochMillisRange()

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