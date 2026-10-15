package com.aljaz.spendingtracker

import com.aljaz.spendingtracker.domain.charts.CategoryBreakdownCalculator
import com.aljaz.spendingtracker.domain.model.CategoryTotal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Checks how expenses are turned into pie slices and the list. */
class CategoryBreakdownTest {

    @Test
    fun emptyPeriodGivesNothing() {
        val result = CategoryBreakdownCalculator.calculate(emptyList())
        assertEquals(0L, result.totalMinor)
        assertTrue(result.slices.isEmpty())
    }

    @Test
    fun mergesSmallCategoriesIntoOther() {
        val result = CategoryBreakdownCalculator.calculate(
            listOf(
                CategoryTotal(1, 6000), // 60 %
                CategoryTotal(2, 3400), // 34 %
                CategoryTotal(3, 300),  //  3 %
                CategoryTotal(4, 300)   //  3 %
            )
        )
        assertEquals(3, result.slices.size)
        val other = result.slices.last()
        assertTrue(other.isOther)
        assertEquals(600L, other.amountMinor)
        // The list still shows every category individually.
        assertEquals(4, result.legend.size)
    }

    @Test
    fun keepsASingleSmallCategoryAsItself() {
        val result = CategoryBreakdownCalculator.calculate(
            listOf(CategoryTotal(1, 9700), CategoryTotal(2, 300))
        )
        assertEquals(2, result.slices.size)
        assertEquals(2L, result.slices.last().categoryId)
    }

    @Test
    fun legendShowsTenCategoriesAndOther() {
        val totals = (1L..12L).map { CategoryTotal(it, 1000 + it) }
        val result = CategoryBreakdownCalculator.calculate(totals)

        assertEquals(11, result.legend.size) // 10 categories + "Other"
        assertTrue(result.legend.last().isOther)
        assertEquals(result.totalMinor, result.legend.sumOf { it.amountMinor }) // adds up
    }

    @Test
    fun sharesAddUpToOne() {
        val result = CategoryBreakdownCalculator.calculate(
            listOf(CategoryTotal(1, 1234), CategoryTotal(null, 4321), CategoryTotal(3, 999))
        )
        assertEquals(1.0, result.slices.sumOf { it.share }, 0.0001)
    }
}