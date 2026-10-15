package com.aljaz.spendingtracker

import com.aljaz.spendingtracker.domain.model.CategoryFilter
import com.aljaz.spendingtracker.domain.model.Transaction
import com.aljaz.spendingtracker.domain.model.TransactionFilter
import com.aljaz.spendingtracker.domain.model.TransactionType
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Checks which transactions a search filter lets through. */
class TransactionFilterTest {

    private val fuel = Transaction(
        type = TransactionType.EXPENSE,
        amountMinor = 16177,
        currency = "EUR",
        categoryId = 2,
        title = "BS LOGO BREZOVICA",
        note = "Full tank before the trip",
        timestamp = 0
    )

    private val salary = Transaction(
        type = TransactionType.INCOME,
        amountMinor = 150000,
        currency = "EUR",
        categoryId = null,
        title = "Plača oktober",
        timestamp = 0
    )

    @Test
    fun emptyFilterMatchesEverything() {
        assertTrue(TransactionFilter().matches(fuel))
        assertTrue(TransactionFilter().matches(salary))
    }

    @Test
    fun findsTitleIgnoringCaseIncludingSlovenianLetters() {
        assertTrue(TransactionFilter(query = "logo").matches(fuel))
        assertTrue(TransactionFilter(query = "PLAČA").matches(salary))
        assertFalse(TransactionFilter(query = "petrol").matches(fuel))
    }

    @Test
    fun findsTextInTheNote() {
        assertTrue(TransactionFilter(query = "tank").matches(fuel))
    }

    @Test
    fun findsAnExactAmountWithCommaOrDot() {
        assertTrue(TransactionFilter(query = "161,77").matches(fuel))
        assertTrue(TransactionFilter(query = "161.77").matches(fuel))
        assertFalse(TransactionFilter(query = "161,78").matches(fuel))
    }

    @Test
    fun filtersByType() {
        val incomeOnly = TransactionFilter(type = TransactionType.INCOME)
        assertFalse(incomeOnly.matches(fuel))
        assertTrue(incomeOnly.matches(salary))
    }

    @Test
    fun filtersByCategory() {
        assertTrue(TransactionFilter(category = CategoryFilter.Specific(2)).matches(fuel))
        assertFalse(TransactionFilter(category = CategoryFilter.Specific(3)).matches(fuel))
        assertTrue(TransactionFilter(category = CategoryFilter.Uncategorized).matches(salary))
        assertFalse(TransactionFilter(category = CategoryFilter.Uncategorized).matches(fuel))
    }

    @Test
    fun allCriteriaMustMatch() {
        val filter = TransactionFilter(
            query = "logo",
            type = TransactionType.EXPENSE,
            category = CategoryFilter.Specific(3) // wrong category
        )
        assertFalse(filter.matches(fuel))
    }
}