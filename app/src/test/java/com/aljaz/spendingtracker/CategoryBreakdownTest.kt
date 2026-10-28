package com.aljaz.spendingtracker

import com.aljaz.spendingtracker.domain.charts.CategoryBreakdownCalculator
import com.aljaz.spendingtracker.domain.model.CategoryTotal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Checks how expenses are turned into pie slices (which the list also shows). */
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
    }

    @Test
    fun keepsASingleSmallCategoryAsItself() {
        val result = CategoryBreakdownCalculator.calculate(
            listOf(CategoryTotal(1, 9700), CategoryTotal(2, 300))
        )
        assertEquals(2, result.slices.size)
        assertEquals(2L, result.slices.last().categoryId)
        assertFalse(result.slices.last().isOther)
    }

    @Test
    fun slicesAddUpToTheTotal() {
        val totals = listOf(
            CategoryTotal(1, 5000), CategoryTotal(2, 2500), CategoryTotal(3, 1500),
            CategoryTotal(4, 400), CategoryTotal(5, 350), CategoryTotal(6, 250)
        )
        val result = CategoryBreakdownCalculator.calculate(totals)

        assertEquals(result.totalMinor, result.slices.sumOf { it.amountMinor })
        assertTrue(result.slices.last().isOther) // 4, 5 and 6 merged
        assertEquals(4, result.slices.size)
    }

    @Test
    fun sharesAddUpToOne() {
        val result = CategoryBreakdownCalculator.calculate(
            listOf(CategoryTotal(1, 1234), CategoryTotal(null, 4321), CategoryTotal(3, 999))
        )
        assertEquals(1.0, result.slices.sumOf { it.share }, 0.0001)
    }
}