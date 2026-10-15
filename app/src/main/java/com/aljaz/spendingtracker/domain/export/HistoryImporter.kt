package com.aljaz.spendingtracker.domain.export

import java.io.InputStream

/**
 * What happened during an import.
 *
 * @property imported transactions added to the app
 * @property skippedDuplicates rows that were already in the app
 * @property createdCategories categories that did not exist and were created
 * @property errors one readable message per row that could not be read
 */
/**
 * What happened during an import. A complete backup can contain all
 * three kinds of data, so the counts are kept separately.
 *
 * @property transactionsImported transactions added to the app
 * @property duplicatesSkipped transactions that were already in the app
 * @property categoriesAdded categories from the file that didn't exist yet
 * @property categoriesRecolored existing categories that took the file's color
 * @property rulesImported keyword rules added or replaced
 * @property categoriesCreated categories created because a transaction or rule needed them
 * @property errors one readable message per row that could not be read
 */
data class ImportSummary(
    val transactionsImported: Int = 0,
    val duplicatesSkipped: Int = 0,
    val categoriesAdded: Int = 0,
    val categoriesRecolored: Int = 0,
    val rulesImported: Int = 0,
    val categoriesCreated: Int = 0,
    val errors: List<String> = emptyList()
) {
    /** Adds up two summaries, e.g. from different sections of one file. */
    operator fun plus(other: ImportSummary) = ImportSummary(
        transactionsImported = transactionsImported + other.transactionsImported,
        duplicatesSkipped = duplicatesSkipped + other.duplicatesSkipped,
        categoriesAdded = categoriesAdded + other.categoriesAdded,
        categoriesRecolored = categoriesRecolored + other.categoriesRecolored,
        rulesImported = rulesImported + other.rulesImported,
        categoriesCreated = categoriesCreated + other.categoriesCreated,
        errors = errors + other.errors
    )
}
/**
 * Reads a transaction history and adds it to the app.
 * The counterpart of [HistoryExporter].
 */
interface HistoryImporter {

    /** Reads from [input] (the caller opens and closes it) and adds new transactions. */
    suspend fun import(input: InputStream): ImportSummary
}