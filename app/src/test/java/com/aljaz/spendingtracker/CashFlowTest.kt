package com.aljaz.spendingtracker

import com.aljaz.spendingtracker.domain.charts.AxisScale
import com.aljaz.spendingtracker.domain.charts.CashFlowCalculator
import com.aljaz.spendingtracker.domain.model.Transaction
import com.aljaz.spendingtracker.domain.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneOffset

/** Checks the cash flow grouping and the Y axis scale. */
class CashFlowTest {

    private val utc = ZoneOffset.UTC

    private fun transaction(type: TransactionType, amountMinor: Long, year: Int, month: Int, day: Int) =
        Transaction(
            type = type,
            amountMinor = amountMinor,
            currency = "EUR",
            title = "Test",
            timestamp = LocalDateTime.of(year, month, day, 12, 0).toInstant(utc).toEpochMilli()
        )

    @Test
    fun listsMonthsOldestFirst() {
        val months = CashFlowCalculator.months(YearMonth.of(2026, 10), 12)
        assertEquals(12, months.size)
        assertEquals(YearMonth.of(2025, 11), months.first())
        assertEquals(YearMonth.of(2026, 10), months.last())
    }

    @Test
    fun groupsTransactionsByCalendarMonth() {
        val months = listOf(YearMonth.of(2026, 9), YearMonth.of(2026, 10))
        val points = CashFlowCalculator.calculate(
            listOf(
                transaction(TransactionType.INCOME, 150000, 2026, 10, 1),
                transaction(TransactionType.EXPENSE, 2000, 2026, 10, 20),
                transaction(TransactionType.EXPENSE, 500, 2026, 9, 30)
            ),
            months, startDay = 1, zone = utc
        )
        assertEquals(500L, points[0].expenseMinor)
        assertEquals(0L, points[0].incomeMinor) // a period without income still gets a point
        assertEquals(150000L, points[1].incomeMinor)
        assertEquals(2000L, points[1].expenseMinor)
    }

    @Test
    fun respectsTheMonthStartDay() {
        val months = listOf(YearMonth.of(2026, 9), YearMonth.of(2026, 10))
        val points = CashFlowCalculator.calculate(
            // 10 October belongs to the September period when periods start on the 15th.
            listOf(transaction(TransactionType.EXPENSE, 1000, 2026, 10, 10)),
            months, startDay = 15, zone = utc
        )
        assertEquals(1000L, points[0].expenseMinor)
        assertEquals(0L, points[1].expenseMinor)
    }

    @Test
    fun choosesRoundAxisMaximums() {
        assertEquals(1000_00L, AxisScale.niceMaxMinor(820_00))  // steps of €250
        assertEquals(2000_00L, AxisScale.niceMaxMinor(1500_00)) // steps of €500
        assertEquals(40_00L, AxisScale.niceMaxMinor(37_00))     // steps of €10
        assertEquals(400_00L, AxisScale.niceMaxMinor(0))        // empty chart
    }
}