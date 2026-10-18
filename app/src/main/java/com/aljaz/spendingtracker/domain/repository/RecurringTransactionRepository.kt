package com.aljaz.spendingtracker.domain.repository

import com.aljaz.spendingtracker.domain.model.RecurringTransaction
import kotlinx.coroutines.flow.Flow

/** Reads and writes recurring transaction templates. */
interface RecurringTransactionRepository {

    /** Saves a new template and returns its id. */
    suspend fun add(recurring: RecurringTransaction): Long

    /** Saves changes to an existing template (matched by id). */
    suspend fun update(recurring: RecurringTransaction)

    /** Stops a series. Transactions it already created are kept. */
    suspend fun delete(recurring: RecurringTransaction)

    /** Every template, used by the background job. */
    suspend fun getAll(): List<RecurringTransaction>

    /** Every template, sorted by title; updates automatically. */
    fun observeAll(): Flow<List<RecurringTransaction>>
}