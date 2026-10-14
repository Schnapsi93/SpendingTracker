package com.aljaz.spendingtracker.domain.usecase

import com.aljaz.spendingtracker.core.AppResult
import com.aljaz.spendingtracker.domain.export.HistoryExporter
import java.io.IOException
import java.io.OutputStream
import javax.inject.Inject

/**
 * Exports the complete history, turning write failures into a
 * readable error instead of a crash.
 */
class ExportHistoryUseCase @Inject constructor(
    private val exporter: HistoryExporter
) {

    /** @return [AppResult.Success] with the number of exported transactions */
    suspend operator fun invoke(output: OutputStream): AppResult<Int> =
        try {
            AppResult.Success(exporter.export(output))
        } catch (e: IOException) {
            AppResult.Error("Could not write the file: ${e.message}")
        }
}