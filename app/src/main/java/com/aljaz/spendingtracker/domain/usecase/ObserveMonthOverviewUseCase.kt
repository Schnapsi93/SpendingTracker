package com.aljaz.spendingtracker.domain.usecase

import com.aljaz.spendingtracker.core.toEpochMillisRange
import com.aljaz.spendingtracker.domain.model.CategoryTotal
import com.aljaz.spendingtracker.domain.model.MonthOverview
import com.aljaz.spendingtracker.domain.model.MonthSummary
import com.aljaz.spendingtracker.domain.model.TransactionType
import com.aljaz.spendingtracker.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import java.time.YearMonth
import javax.inject.Inject

/**
 * Streams the transactions, totals and per-category spending for one
 * period (a calendar month, or e.g. 15th to 14th, see MonthRange.kt).
 * Emits a new value every time the underlying data changes.
 */
class ObserveMonthOverviewUseCase @Inject constructor(
    private val repository: TransactionRepository
) {

    /**
     * @param month the period to observe, named after the month it starts in
     * @param startDay the day of the month periods start on; 1 = calendar months
     * @param carryOver whether to include the balance left over from earlier periods
     */
    operator fun invoke(
        month: YearMonth,
        startDay: Int = 1,
        carryOver: Boolean = false
    ): Flow<MonthOverview> {
        val (start, end) = month.toEpochMillisRange(startDay = startDay)

        // Everything before the period start, or simply 0 when carry-over is off.
        val carriedOver: Flow<Long> =
            if (carryOver) repository.observeBalanceBefore(start) else flowOf(0L)

        return combine(repository.observeBetween(start, end), carriedOver) { transactions, carried ->
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
                    totalExpenseMinor = expenses.sumOf { it.amountMinor },
                    carriedOverMinor = carried
                ),
                transactions = transactions,
                expenseByCategory = expenseByCategory
            )
        }
    }
}