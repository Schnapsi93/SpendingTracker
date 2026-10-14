package com.aljaz.spendingtracker.domain.repository

import com.aljaz.spendingtracker.domain.model.Transaction
import kotlinx.coroutines.flow.Flow

/**
 * The single entry point for reading and writing transactions.
 * The rest of the app depends on this interface, never on Room directly.
 */
interface TransactionRepository {

    /**
     * Saves a new transaction.
     *
     * @return the new id, or null if it was rejected as a duplicate
     */
    suspend fun add(transaction: Transaction): Long?

    /** Saves changes to an existing transaction. */
    suspend fun update(transaction: Transaction)

    /** Permanently deletes a transaction. */
    suspend fun delete(transaction: Transaction)

    /** Returns one transaction, or null if it doesn't exist. */
    suspend fun getById(id: Long): Transaction?

    /**
     * Streams the transactions between two moments, newest first.
     * Emits a new list every time the data changes.
     *
     * @param startMillis range start (inclusive), UTC epoch millis
     * @param endMillis range end (exclusive), UTC epoch millis
     */
    fun observeBetween(startMillis: Long, endMillis: Long): Flow<List<Transaction>>
}