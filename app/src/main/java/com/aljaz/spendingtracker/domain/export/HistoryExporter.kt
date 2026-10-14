package com.aljaz.spendingtracker.domain.export

import java.io.OutputStream

/**
 * Writes the complete transaction history somewhere.
 *
 * The domain layer only knows this interface; the actual file format
 * (CSV today, maybe JSON later) is an implementation detail.
 */
interface HistoryExporter {

    /**
     * Writes all transactions to [output]. The caller opens and closes the stream.
     *
     * @return the number of transactions written
     */
    suspend fun export(output: OutputStream): Int
}