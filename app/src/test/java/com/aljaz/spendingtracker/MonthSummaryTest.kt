package com.aljaz.spendingtracker

import com.aljaz.spendingtracker.domain.model.MonthSummary
import org.junit.Assert.assertEquals
import org.junit.Test

/** Checks how the period balance is calculated. */
class MonthSummaryTest {

    @Test
    fun balanceWithoutCarryOver() {
        val summary = MonthSummary(totalIncomeMinor = 150000, totalExpenseMinor = 82040)
        assertEquals(67960L, summary.balanceMinor)
    }

    @Test
    fun positiveCarryOverIsAdded() {
        val summary = MonthSummary(
            totalIncomeMinor = 150000,
            totalExpenseMinor = 82040,
            carriedOverMinor = 12000
        )
        assertEquals(79960L, summary.balanceMinor)
    }

    @Test
    fun negativeCarryOverIsSubtracted() {
        val summary = MonthSummary(
            totalIncomeMinor = 0,
            totalExpenseMinor = 1000,
            carriedOverMinor = -5000
        )
        assertEquals(-6000L, summary.balanceMinor)
    }
}