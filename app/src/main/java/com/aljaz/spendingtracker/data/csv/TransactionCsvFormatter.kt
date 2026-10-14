package com.aljaz.spendingtracker.data.csv

import com.aljaz.spendingtracker.domain.model.Category
import com.aljaz.spendingtracker.domain.model.Transaction
import com.aljaz.spendingtracker.domain.model.TransactionType
import java.math.BigDecimal
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject

/**
 * Turns transactions into CSV text following [CsvFormat].
 *
 * It only builds the text. Writing it to a file or uploading it is
 * done elsewhere, which keeps this class simple to test.
 */
class TransactionCsvFormatter @Inject constructor() {

    /**
     * @param transactions the transactions to include, in any order
     * @param categoriesById used to write category names instead of ids
     * @param zone time zone for the Date column (the phone's by default)
     * @return the complete CSV text: header row, then one row per
     *         transaction, oldest first. The BOM is not included; the
     *         code that writes the file adds it.
     */
    fun format(
        transactions: List<Transaction>,
        categoriesById: Map<Long, Category>,
        zone: ZoneId = ZoneId.systemDefault()
    ): String = buildString {
        appendRow(CsvFormat.HEADER)
        transactions
            .sortedBy { it.timestamp }
            .forEach { transaction ->
                appendRow(toFields(transaction, categoriesById, zone))
            }
    }

    /** Converts one transaction into its column values, in [CsvFormat.HEADER] order. */
    private fun toFields(
        transaction: Transaction,
        categoriesById: Map<Long, Category>,
        zone: ZoneId
    ): List<String> = listOf(
        Instant.ofEpochMilli(transaction.timestamp)
            .atZone(zone)
            .format(CsvFormat.DATE_TIME_FORMAT),
        when (transaction.type) {
            TransactionType.EXPENSE -> CsvFormat.TYPE_EXPENSE
            TransactionType.INCOME -> CsvFormat.TYPE_INCOME
        },
        formatAmount(transaction.amountMinor),
        transaction.currency,
        transaction.categoryId?.let { categoriesById[it]?.name }.orEmpty(),
        transaction.title,
        transaction.note.orEmpty(),
        transaction.source.name
    )

    /** Writes 1127 as "11,27": always two decimals, decimal comma, no grouping. */
    private fun formatAmount(amountMinor: Long): String =
        BigDecimal.valueOf(amountMinor, 2).toPlainString().replace('.', ',')

    /** Appends one row: escaped values joined by the separator, then a line end. */
    private fun StringBuilder.appendRow(fields: List<String>) {
        append(fields.joinToString(CsvFormat.SEPARATOR.toString()) { escapeCsvField(it) })
        append(CsvFormat.LINE_END)
    }
}