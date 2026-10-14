package com.aljaz.spendingtracker.data.csv

import com.aljaz.spendingtracker.domain.export.HistoryImporter
import com.aljaz.spendingtracker.domain.export.ImportSummary
import com.aljaz.spendingtracker.domain.model.Category
import com.aljaz.spendingtracker.domain.model.Transaction
import com.aljaz.spendingtracker.domain.model.TransactionType
import com.aljaz.spendingtracker.domain.repository.CategoryRepository
import com.aljaz.spendingtracker.domain.repository.TransactionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.Charset
import java.nio.charset.CodingErrorAction
import javax.inject.Inject

/**
 * [HistoryImporter] for CSV files in [CsvFormat].
 *
 * Only adds data: existing transactions are never changed or deleted,
 * and rows that are already in the app are skipped.
 */
class CsvHistoryImporter @Inject constructor(
    private val parser: TransactionCsvParser,
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository
) : HistoryImporter {

    override suspend fun import(input: InputStream): ImportSummary {
        val bytes = withContext(Dispatchers.IO) { input.readBytes() }
        val parsed = parser.parse(decode(bytes))

        // Fingerprints of everything already stored, to recognize duplicates.
        val existingKeys = transactionRepository.getAll()
            .map { it.duplicateKey() }
            .toHashSet()

        // Categories by type + lowercase name; new ones are added as they're created.
        val categories = categoryRepository.observeAll().first()
            .associateBy { categoryKey(it.type, it.name) }
            .toMutableMap()

        var imported = 0
        var duplicates = 0
        var createdCategories = 0

        for (row in parsed.rows) {
            val transaction = row.transaction
            if (transaction.duplicateKey() in existingKeys) {
                duplicates++
                continue
            }

            // Find the category by name, or create it if it doesn't exist yet.
            val categoryId: Long? = row.categoryName?.let { name ->
                val key = categoryKey(transaction.type, name)
                categories[key]?.id ?: run {
                    val newCategory = Category(
                        name = name,
                        type = transaction.type,
                        colorArgb = NEW_CATEGORY_COLOR
                    )
                    categoryRepository.add(newCategory)?.also { id ->
                        categories[key] = newCategory.copy(id = id)
                        createdCategories++
                    }
                }
            }

            if (transactionRepository.add(transaction.copy(categoryId = categoryId)) != null) {
                imported++
            }
        }

        return ImportSummary(imported, duplicates, createdCategories, parsed.errors)
    }

    /**
     * Fingerprint used to recognize the same transaction in the app and the file.
     * Time is compared to the minute, because the CSV doesn't store seconds.
     */
    private fun Transaction.duplicateKey(): String =
        listOf(timestamp / 60_000, type, amountMinor, currency, title.trim().lowercase())
            .joinToString("|")

    private fun categoryKey(type: TransactionType, name: String): String =
        "${type.name}|${name.trim().lowercase()}"

    /**
     * Turns file bytes into text. Tries UTF-8 first (the app's own files);
     * if that fails, falls back to Windows-1250, which Excel uses on
     * Slovenian systems when it re-saves a CSV.
     */
    private fun decode(bytes: ByteArray): String =
        try {
            Charsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(bytes))
                .toString()
        } catch (e: CharacterCodingException) {
            String(bytes, Charset.forName("windows-1250"))
        }

    private companion object {
        /** Neutral grey for categories created by an import; recolor them later. */
        val NEW_CATEGORY_COLOR: Int = 0xFF9E9E9E.toInt()
    }
}