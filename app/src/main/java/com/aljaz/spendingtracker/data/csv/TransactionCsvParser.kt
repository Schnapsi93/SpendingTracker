package com.aljaz.spendingtracker.data.csv

import com.aljaz.spendingtracker.core.AppResult
import com.aljaz.spendingtracker.core.MoneyParser
import com.aljaz.spendingtracker.domain.model.Transaction
import com.aljaz.spendingtracker.domain.model.TransactionSource
import com.aljaz.spendingtracker.domain.model.TransactionType
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeParseException
import javax.inject.Inject

/**
 * One successfully read CSV row, before its category is matched
 * against the database.
 *
 * @property transaction the transaction, without id or category id yet
 * @property categoryName the name from the Category column, or null if empty
 */
data class CsvTransactionRow(
    val transaction: Transaction,
    val categoryName: String?
)

/**
 * Result of reading a whole file.
 *
 * @property rows every row that could be read
 * @property errors one readable message per row that could not be read
 */
data class CsvParseResult(
    val rows: List<CsvTransactionRow>,
    val errors: List<String>
)

/**
 * Reads CSV text written in [CsvFormat] back into transactions.
 * The counterpart of [TransactionCsvFormatter].
 */
class TransactionCsvParser @Inject constructor() {

    /**
     * @param text the complete file content
     * @param zone time zone used to interpret the Date column
     */
    fun parse(text: String, zone: ZoneId = ZoneId.systemDefault()): CsvParseResult {
        val table = parseCsv(text)
        if (table.isEmpty()) {
            return CsvParseResult(emptyList(), listOf("The file is empty."))
        }

        val header = table.first().map { it.trim().lowercase() }
        if (header != CsvFormat.HEADER.map { it.lowercase() }) {
            return CsvParseResult(
                emptyList(),
                listOf("Unexpected columns. Expected: ${CsvFormat.HEADER.joinToString(";")}")
            )
        }

        val rows = mutableListOf<CsvTransactionRow>()
        val errors = mutableListOf<String>()

        // Row numbers count data rows only, starting at 1 below the header.
        table.drop(1).forEachIndexed { index, fields ->
            when (val result = parseRow(fields, zone)) {
                is AppResult.Success -> rows.add(result.data)
                is AppResult.Error -> errors.add("Row ${index + 1}: ${result.message}")
            }
        }
        return CsvParseResult(rows, errors)
    }

    /** Converts one row's fields, in [CsvFormat.HEADER] order. */
    private fun parseRow(fields: List<String>, zone: ZoneId): AppResult<CsvTransactionRow> {
        if (fields.size != CsvFormat.HEADER.size) {
            return AppResult.Error(
                "expected ${CsvFormat.HEADER.size} columns, found ${fields.size}"
            )
        }

        val timestamp = parseDate(fields[0], zone)
            ?: return AppResult.Error("invalid date '${fields[0]}'")

        val type = when (fields[1].trim().lowercase()) {
            CsvFormat.TYPE_EXPENSE.lowercase() -> TransactionType.EXPENSE
            CsvFormat.TYPE_INCOME.lowercase() -> TransactionType.INCOME
            else -> return AppResult.Error("unknown type '${fields[1]}'")
        }

        val amountMinor = MoneyParser.parseToMinor(fields[2])
            ?: return AppResult.Error("invalid amount '${fields[2]}'")

        val currency = fields[3].trim().uppercase()
        if (currency.isEmpty()) {
            return AppResult.Error("missing currency")
        }

        // Unknown or missing sources are marked as imported.
        val source = TransactionSource.entries
            .find { it.name == fields[7].trim().uppercase() }
            ?: TransactionSource.IMPORT

        val transaction = Transaction(
            type = type,
            amountMinor = amountMinor,
            currency = currency,
            title = fields[5].trim().ifEmpty {
                if (type == TransactionType.EXPENSE) "Expense" else "Income"
            },
            note = fields[6].trim().ifEmpty { null },
            timestamp = timestamp,
            source = source
        )
        return AppResult.Success(
            CsvTransactionRow(transaction, categoryName = fields[4].trim().ifEmpty { null })
        )
    }

    /** Tries every accepted date format; returns epoch millis, or null if none fits. */
    private fun parseDate(text: String, zone: ZoneId): Long? {
        val trimmed = text.trim()
        for (format in CsvFormat.IMPORT_DATE_TIME_FORMATS) {
            try {
                return LocalDateTime.parse(trimmed, format)
                    .atZone(zone)
                    .toInstant()
                    .toEpochMilli()
            } catch (e: DateTimeParseException) {
                // Not this format; try the next one.
            }
        }
        return null
    }
}