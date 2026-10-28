package com.aljaz.spendingtracker.domain.sharing

import com.aljaz.spendingtracker.core.AppResult
import kotlinx.coroutines.flow.Flow

/**
 * Settle-up payments of the household. Recording and undoing go straight
 * to the server, so they need an internet connection.
 */
interface SettlementRepository {

    /** Every payment, newest first; updates automatically. */
    fun observeAll(): Flow<List<Settlement>>

    /**
     * Records that [fromUser] paid [toUser] [amountMinor].
     *
     * @return [AppResult.Error] with a readable message, e.g. when offline
     */
    suspend fun record(
        fromUser: String,
        toUser: String,
        amountMinor: Long,
        note: String?
    ): AppResult<Unit>

    /** Undoes a payment recorded by mistake (on both phones). */
    suspend fun delete(settlement: Settlement): AppResult<Unit>
}