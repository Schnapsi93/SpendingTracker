package com.aljaz.spendingtracker.domain.sharing

import kotlinx.coroutines.flow.Flow

/** Read access to the household's shared expenses (the synced mirror). */
interface SharedExpenseRepository {

    /**
     * Shared expenses between two moments, newest first; updates automatically.
     *
     * @param startMillis range start (inclusive), UTC epoch millis
     * @param endMillis range end (exclusive), UTC epoch millis
     */
    fun observeBetween(startMillis: Long, endMillis: Long): Flow<List<SharedExpense>>

    /** Every shared expense, newest first; updates automatically. */
    fun observeAll(): Flow<List<SharedExpense>>
}