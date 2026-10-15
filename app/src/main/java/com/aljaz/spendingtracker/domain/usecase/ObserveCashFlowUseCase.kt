package com.aljaz.spendingtracker.domain.usecase

import com.aljaz.spendingtracker.core.toEpochMillisRange
import com.aljaz.spendingtracker.domain.charts.CashFlowCalculator
import com.aljaz.spendingtracker.domain.charts.CashFlowPoint
import com.aljaz.spendingtracker.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.YearMonth
import java.time.ZoneId
import javax.inject.Inject

/**
 * Streams income and expenses for several consecutive periods, for the
 * cash flow chart. Emits again whenever the data changes.
 */
class ObserveCashFlowUseCase @Inject constructor(
    private val repository: TransactionRepository
) {

    /**
     * @param endMonth the last (newest) period shown
     * @param startDay the day of the month periods start on
     * @param count how many periods, ending with [endMonth]
     * @return one point per period, oldest first
     */
    operator fun invoke(
        endMonth: YearMonth,
        startDay: Int,
        count: Int = PERIOD_COUNT,
        zone: ZoneId = ZoneId.systemDefault()
    ): Flow<List<CashFlowPoint>> {
        val months = CashFlowCalculator.months(endMonth, count)
        // One query for the whole range, from the first period's start to the last one's end.
        val start = months.first().toEpochMillisRange(zone, startDay).first
        val end = months.last().toEpochMillisRange(zone, startDay).second

        return repository.observeBetween(start, end).map { transactions ->
            CashFlowCalculator.calculate(transactions, months, startDay, zone)
        }
    }

    companion object {
        /** The chart shows this many periods. */
        const val PERIOD_COUNT = 12
    }
}