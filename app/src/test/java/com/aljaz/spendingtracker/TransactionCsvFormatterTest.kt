package com.aljaz.spendingtracker

import com.aljaz.spendingtracker.data.csv.TransactionCsvFormatter
import com.aljaz.spendingtracker.domain.model.Category
import com.aljaz.spendingtracker.domain.model.Transaction
import com.aljaz.spendingtracker.domain.model.TransactionSource
import com.aljaz.spendingtracker.domain.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneOffset

/** Checks that transactions are written in the agreed CSV format. */
class TransactionCsvFormatterTest {

    private val formatter = TransactionCsvFormatter()

    // Tests use UTC so the results don't depend on the computer's time zone.
    private val utc = ZoneOffset.UTC

    private val fuel = Category(
        id = 1, name = "Car / Fuel", type = TransactionType.EXPENSE, colorArgb = 0
    )

    /** Epoch millis for a date and time in UTC. */
    private fun millis(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long =
        LocalDateTime.of(year, month, day, hour, minute).toInstant(ZoneOffset.UTC).toEpochMilli()

    /** A transaction with sensible defaults; tests override what they check. */
    private fun transaction(
        title: String = "Test",
        amountMinor: Long = 100,
        type: TransactionType = TransactionType.EXPENSE,
        categoryId: Long? = null,
        timestamp: Long = millis(2026, 10, 1, 12, 0),
        source: TransactionSource = TransactionSource.MANUAL
    ) = Transaction(
        type = type,
        amountMinor = amountMinor,
        currency = "EUR",
        categoryId = categoryId,
        title = title,
        timestamp = timestamp,
        source = source
    )

    /** Formats and splits into lines (the last element is empty after the final line end). */
    private fun lines(vararg transactions: Transaction): List<String> =
        formatter.format(transactions.toList(), mapOf(1L to fuel), utc).split("\r\n")

    @Test
    fun writesHeaderFirst() {
        assertEquals("Date;Type;Amount;Currency;Category;Title;Note;Source", lines()[0])
    }

    @Test
    fun writesSmsExpenseWithDecimalComma() {
        val row = lines(
            transaction(
                title = "BS LOGO BREZOVICA",
                amountMinor = 1127,
                categoryId = 1,
                timestamp = millis(2026, 10, 11, 19, 38),
                source = TransactionSource.SMS
            )
        )[1]
        assertEquals("2026-10-11 19:38;Expense;11,27;EUR;Car / Fuel;BS LOGO BREZOVICA;;SMS", row)
    }

    @Test
    fun writesIncomeAndWholeAmounts() {
        val row = lines(transaction(type = TransactionType.INCOME, amountMinor = 150000))[1]
        assertEquals("Income", row.split(";")[1])
        assertEquals("1500,00", row.split(";")[2])
    }

    @Test
    fun leavesCategoryEmptyWhenUncategorized() {
        val row = lines(transaction(categoryId = null))[1]
        assertEquals("", row.split(";")[4])
    }

    @Test
    fun quotesFieldsContainingSeparatorOrQuotes() {
        val row = lines(transaction(title = "Bar; \"Pri Joži\""))[1]
        assertEquals(true, row.contains(";\"Bar; \"\"Pri Joži\"\"\";"))
    }

    @Test
    fun sortsOldestFirst() {
        val result = lines(
            transaction(title = "Newer", timestamp = millis(2026, 10, 20, 10, 0)),
            transaction(title = "Older", timestamp = millis(2026, 10, 5, 10, 0))
        )
        assertEquals(true, result[1].contains("Older"))
        assertEquals(true, result[2].contains("Newer"))
    }
}