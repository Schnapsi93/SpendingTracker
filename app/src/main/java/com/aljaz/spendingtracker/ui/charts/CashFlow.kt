package com.aljaz.spendingtracker.domain.charts

import com.aljaz.spendingtracker.core.periodContaining
import com.aljaz.spendingtracker.domain.model.Transaction
import com.aljaz.spendingtracker.domain.model.TransactionType
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow

/**
 * Income and expenses of one period, for the cash flow chart.
 *
 * @property month the period, named after the month it starts in
 */
data class CashFlowPoint(
    val month: YearMonth,
    val incomeMinor: Long,
    val expenseMinor: Long
)

/** Groups transactions into consecutive periods. */
object CashFlowCalculator {

    /** The [count] periods ending with [endMonth], oldest first. */
    fun months(endMonth: YearMonth, count: Int): List<YearMonth> =
        (count - 1 downTo 0).map { endMonth.minusMonths(it.toLong()) }

    /**
     * @param transactions transactions within the range of [months]
     * @param months the periods to fill, oldest first; periods without
     *        transactions get zeros, so the chart has no gaps
     * @param startDay the day of the month periods start on
     */
    fun calculate(
        transactions: List<Transaction>,
        months: List<YearMonth>,
        startDay: Int,
        zone: ZoneId = ZoneId.systemDefault()
    ): List<CashFlowPoint> {
        val byPeriod = transactions.groupBy { transaction ->
            val date = Instant.ofEpochMilli(transaction.timestamp).atZone(zone).toLocalDate()
            periodContaining(date, startDay)
        }
        return months.map { month ->
            val items = byPeriod[month].orEmpty()
            CashFlowPoint(
                month = month,
                incomeMinor = items.filter { it.type == TransactionType.INCOME }.sumOf { it.amountMinor },
                expenseMinor = items.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amountMinor }
            )
        }
    }
}

/** Chooses "round" values for a chart's Y axis. */
object AxisScale {

    /** Number of equal steps between 0 and the top of the axis. */
    const val STEPS = 4

    /**
     * The smallest round axis maximum that fits [maxMinor]: [STEPS] equal
     * steps of 1, 2, 2.5 or 5 times a power of ten, in whole euros.
     * Examples: €820 → €1,000 (steps of €250); €1,500 → €2,000 (steps of €500).
     *
     * @return the axis maximum in minor units; €400 for an empty chart
     */
    fun niceMaxMinor(maxMinor: Long, steps: Int = STEPS): Long {
        if (maxMinor <= 0) return steps * 100_00L // €100 steps when there is no data

        val maxEuros = ceil(maxMinor / 100.0)
        val rawStep = maxEuros / steps
        val magnitude = 10.0.pow(floor(log10(rawStep)))
        val niceStep = listOf(1.0, 2.0, 2.5, 5.0, 10.0)
            .map { it * magnitude }
            .first { it >= rawStep }
            .coerceAtLeast(1.0) // never finer than €1 steps

        return (niceStep * steps * 100).toLong()
    }
}