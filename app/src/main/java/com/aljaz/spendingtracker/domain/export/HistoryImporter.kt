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
data class ImportSummary(
    val imported: Int,
    val skippedDuplicates: Int,
    val createdCategories: Int,
    val errors: List<String>
)

/**
 * Reads a transaction history and adds it to the app.
 * The counterpart of [HistoryExporter].
 */
interface HistoryImporter {

    /** Reads from [input] (the caller opens and closes it) and adds new transactions. */
    suspend fun import(input: InputStream): ImportSummary
}