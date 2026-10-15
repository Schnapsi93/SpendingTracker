package com.aljaz.spendingtracker

import com.aljaz.spendingtracker.data.csv.ConfigurationCsvFormatter
import com.aljaz.spendingtracker.data.csv.ConfigurationCsvParser
import com.aljaz.spendingtracker.data.csv.CsvFormat
import com.aljaz.spendingtracker.data.csv.parseCsv
import com.aljaz.spendingtracker.domain.model.Category
import com.aljaz.spendingtracker.domain.model.CategoryRule
import com.aljaz.spendingtracker.domain.model.RuleField
import com.aljaz.spendingtracker.domain.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Checks the categories and rules backup files. */
class ConfigurationCsvTest {

    private val formatter = ConfigurationCsvFormatter()
    private val parser = ConfigurationCsvParser()

    private val fuel = Category(
        id = 1, name = "Car / Fuel", type = TransactionType.EXPENSE,
        colorArgb = 0xFFF44336.toInt()
    )
    private val salary = Category(
        id = 2, name = "Salary", type = TransactionType.INCOME,
        colorArgb = 0xFF388E3C.toInt()
    )

    @Test
    fun writesAndReadsColors() {
        assertEquals("#F44336", CsvFormat.formatColor(0xFFF44336.toInt()))
        assertEquals(0xFFF44336.toInt(), CsvFormat.parseColor("#f44336"))
        assertNull(CsvFormat.parseColor("red"))
    }

    @Test
    fun categoriesSurviveARoundTrip() {
        val csv = formatter.formatCategories(listOf(salary, fuel))
        val parsed = parser.parseCategories(parseCsv(csv))

        assertTrue(parsed.errors.isEmpty())
        // Expenses come first in the file.
        assertEquals("Car / Fuel", parsed.rows[0].name)
        assertEquals(TransactionType.EXPENSE, parsed.rows[0].type)
        assertEquals(fuel.colorArgb, parsed.rows[0].colorArgb)
        assertEquals("Salary", parsed.rows[1].name)
    }

    @Test
    fun rulesSurviveARoundTrip() {
        val rules = listOf(
            CategoryRule(keyword = "BS LOGO", categoryId = 1),
            CategoryRule(keyword = "BA DVIG", field = RuleField.OPERATION, categoryId = 1)
        )
        val csv = formatter.formatRules(rules, mapOf(1L to fuel))
        val parsed = parser.parseRules(parseCsv(csv))

        assertTrue(parsed.errors.isEmpty())
        val atm = parsed.rows.first { it.keyword == "BA DVIG" }
        assertEquals(RuleField.OPERATION, atm.field)
        assertEquals("Car / Fuel", atm.categoryName)
        assertEquals(TransactionType.EXPENSE, atm.categoryType)
    }

    @Test
    fun skipsRulesWhoseCategoryIsMissing() {
        val csv = formatter.formatRules(
            listOf(CategoryRule(keyword = "ORPHAN", categoryId = 99)),
            mapOf(1L to fuel)
        )
        assertTrue(parser.parseRules(parseCsv(csv)).rows.isEmpty())
    }

    @Test
    fun reportsBadRows() {
        val csv = "Name;Type;Color\r\nTravel;Holiday;#FF0000\r\n;Expense;#FF0000\r\n"
        val parsed = parser.parseCategories(parseCsv(csv))

        assertTrue(parsed.rows.isEmpty())
        assertEquals(2, parsed.errors.size)
    }
}