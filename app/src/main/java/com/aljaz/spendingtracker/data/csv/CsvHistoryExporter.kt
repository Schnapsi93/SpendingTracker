package com.aljaz.spendingtracker.data.csv

import com.aljaz.spendingtracker.domain.export.HistoryExporter
import com.aljaz.spendingtracker.domain.repository.CategoryRepository
import com.aljaz.spendingtracker.domain.repository.TransactionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.OutputStream
import javax.inject.Inject

/**
 * [HistoryExporter] that writes the history as a CSV file in [CsvFormat].
 */
class CsvHistoryExporter @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val formatter: TransactionCsvFormatter
) : HistoryExporter {

    override suspend fun export(output: OutputStream): Int {
        val transactions = transactionRepository.getAll()
        // first() takes the current list from the stream, then stops listening.
        val categoriesById = categoryRepository.observeAll().first().associateBy { it.id }
        val csv = formatter.format(transactions, categoriesById)

        // File writing is slow work, so it runs on the IO thread pool.
        withContext(Dispatchers.IO) {
            val writer = output.bufferedWriter(Charsets.UTF_8)
            writer.write(CsvFormat.BOM) // makes Excel read č, š, ž correctly
            writer.write(csv)
            writer.flush() // the caller closes the stream itself
        }
        return transactions.size
    }
}