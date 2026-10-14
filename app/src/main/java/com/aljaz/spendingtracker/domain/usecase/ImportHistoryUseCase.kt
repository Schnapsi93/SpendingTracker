package com.aljaz.spendingtracker.domain.usecase

import com.aljaz.spendingtracker.core.AppResult
import com.aljaz.spendingtracker.domain.export.HistoryImporter
import com.aljaz.spendingtracker.domain.export.ImportSummary
import java.io.IOException
import java.io.InputStream
import javax.inject.Inject

/**
 * Imports a history file, turning read failures into a readable
 * error instead of a crash.
 */
class ImportHistoryUseCase @Inject constructor(
    private val importer: HistoryImporter
) {

    suspend operator fun invoke(input: InputStream): AppResult<ImportSummary> =
        try {
            AppResult.Success(importer.import(input))
        } catch (e: IOException) {
            AppResult.Error("Could not read the file: ${e.message}")
        }
}