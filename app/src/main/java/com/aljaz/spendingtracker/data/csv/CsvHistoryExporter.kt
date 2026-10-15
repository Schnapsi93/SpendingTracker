package com.aljaz.spendingtracker.data.csv

import com.aljaz.spendingtracker.domain.export.HistoryExporter
import com.aljaz.spendingtracker.domain.repository.CategoryRepository
import com.aljaz.spendingtracker.domain.repository.CategoryRuleRepository
import com.aljaz.spendingtracker.domain.repository.TransactionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.OutputStream
import javax.inject.Inject

/**
 * [HistoryExporter] that writes a complete backup as one CSV file with
 * three sections: transactions, categories and keyword rules.
 *
 * Transactions come first, so the file is easy to read in Excel.
 */
class CsvHistoryExporter @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val ruleRepository: CategoryRuleRepository,
    private val formatter: TransactionCsvFormatter,
    private val configurationFormatter: ConfigurationCsvFormatter
) : HistoryExporter {

    override suspend fun export(output: OutputStream): Int {
        val transactions = transactionRepository.getAll()
        // first() takes the current list from the stream, then stops listening.
        val categories = categoryRepository.observeAll().first()
        val categoriesById = categories.associateBy { it.id }
        val rules = ruleRepository.getAll()

        val text = buildString {
            append(csvRow(listOf(CsvFormat.SECTION_TRANSACTIONS)))
            append(formatter.format(transactions, categoriesById))
            append(CsvFormat.LINE_END) // empty line between sections, for readability

            append(csvRow(listOf(CsvFormat.SECTION_CATEGORIES)))
            append(configurationFormatter.formatCategories(categories))
            append(CsvFormat.LINE_END)

            append(csvRow(listOf(CsvFormat.SECTION_RULES)))
            append(configurationFormatter.formatRules(rules, categoriesById))
        }

        // File writing is slow work, so it runs on the IO thread pool.
        withContext(Dispatchers.IO) {
            val writer = output.bufferedWriter(Charsets.UTF_8)
            writer.write(CsvFormat.BOM) // makes Excel read č, š, ž correctly
            writer.write(text)
            writer.flush() // the caller closes the stream itself
        }

        // The number of transactions decides the "never upload an empty backup" rule.
        return transactions.size
    }
}