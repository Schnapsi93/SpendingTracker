package com.aljaz.spendingtracker

import com.aljaz.spendingtracker.data.csv.TransactionCsvFormatter
import com.aljaz.spendingtracker.data.csv.TransactionCsvParser
import com.aljaz.spendingtracker.data.csv.parseCsv
import com.aljaz.spendingtracker.domain.model.Category
import com.aljaz.spendingtracker.domain.model.Transaction
import com.aljaz.spendingtracker.domain.model.TransactionSource
import com.aljaz.spendingtracker.domain.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneOffset

/** Checks that CSV files are read back correctly, including tricky cases. */
class TransactionCsvParserTest {

    private val parser = TransactionCsvParser()
    private val formatter = TransactionCsvFormatter()
    private val utc = ZoneOffset.UTC
    private val header = "Date;Type;Amount;Currency;Category;Title;Note;Source\r\n"

    private fun millis(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long =
        LocalDateTime.of(year, month, day, hour, minute).toInstant(ZoneOffset.UTC).toEpochMilli()

    @Test
    fun readsQuotedFieldsWithSeparatorsAndQuotes() {
        val rows = parseCsv("a;\"b;c\";\"say \"\"hi\"\"\"\r\n")
        assertEquals(listOf(listOf("a", "b;c", "say \"hi\"")), rows)
    }

    @Test
    fun ignoresBomAndEmptyLines() {
        val rows = parseCsv("\uFEFFa;b\r\n\r\nc;d\r\n")
        assertEquals(listOf(listOf("a", "b"), listOf("c", "d")), rows)
    }

    @Test
    fun readsBackWhatTheFormatterWrote() {
        val fuel = Category(id = 1, name = "Car / Fuel", type = TransactionType.EXPENSE, colorArgb = 0)
        val original = Transaction(
            type = TransactionType.EXPENSE,
            amountMinor = 1127,
            currency = "EUR",
            categoryId = 1,
            title = "Bar; \"Pri Joži\"",
            note = "with friends",
            timestamp = millis(2026, 10, 11, 19, 38),
            source = TransactionSource.SMS
        )

        val csv = formatter.format(listOf(original), mapOf(1L to fuel), utc)
        val result = parser.parse(csv, utc)

        assertTrue(result.errors.isEmpty())
        val row = result.rows.single()
        assertEquals("Car / Fuel", row.categoryName)
        assertEquals(original.amountMinor, row.transaction.amountMinor)
        assertEquals(original.title, row.transaction.title)
        assertEquals(original.note, row.transaction.note)
        assertEquals(original.timestamp, row.transaction.timestamp)
        assertEquals(original.type, row.transaction.type)
        assertEquals(original.source, row.transaction.source)
    }

    @Test
    fun acceptsExcelStyleDatesAndEmptyCategory() {
        val result = parser.parse(header + "11.10.2026 19:38;Expense;11,27;EUR;;Test;;MANUAL\r\n", utc)
        val row = result.rows.single()
        assertEquals(millis(2026, 10, 11, 19, 38), row.transaction.timestamp)
        assertNull(row.categoryName)
    }

    @Test
    fun reportsBadRowsAndKeepsGoodOnes() {
        val result = parser.parse(
            header +
                    "2026-10-01 10:00;Expense;abc;EUR;;Bad;;MANUAL\r\n" +
                    "2026-10-02 10:00;Expense;5,00;EUR;;Good;;MANUAL\r\n",
            utc
        )
        assertEquals(1, result.rows.size)
        assertEquals("Good", result.rows[0].transaction.title)
        assertTrue(result.errors.single().startsWith("Row 1:"))
    }

    @Test
    fun rejectsFilesWithOtherColumns() {
        val result = parser.parse("Name;Price\r\nBread;2,00\r\n", utc)
        assertTrue(result.rows.isEmpty())
        assertTrue(result.errors.single().startsWith("Unexpected columns"))
    }
}