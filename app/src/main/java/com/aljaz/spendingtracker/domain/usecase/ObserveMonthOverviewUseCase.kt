package com.aljaz.spendingtracker.domain.usecase

import com.aljaz.spendingtracker.core.toEpochMillisRange
import com.aljaz.spendingtracker.domain.model.MonthOverview
import com.aljaz.spendingtracker.domain.model.MonthSummary
import com.aljaz.spendingtracker.domain.model.TransactionType
import com.aljaz.spendingtracker.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.YearMonth
import javax.inject.Inject

/**
 * Streams the transactions and totals for one calendar month.
 * Emits a new value every time the underlying data changes.
 */
class ObserveMonthOverviewUseCase @Inject constructor(
    private val repository: TransactionRepository
) {

    /** @param month the month to observe, e.g. YearMonth.now() */
    operator fun invoke(month: YearMonth): Flow<MonthOverview> {
        val (start, end) = month.toEpochMillisRange()

        return repository.observeBetween(start, end).map { transactions ->
            val income = transactions
                .filter { it.type == TransactionType.INCOME }
                .sumOf { it.amountMinor }
            val expense = transactions
                .filter { it.type == TransactionType.EXPENSE }
                .sumOf { it.amountMinor }

            MonthOverview(
                summary = MonthSummary(income, expense),
                transactions = transactions
            )
        }
    }
}