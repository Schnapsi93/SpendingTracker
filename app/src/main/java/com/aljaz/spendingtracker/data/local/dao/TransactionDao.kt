package com.aljaz.spendingtracker.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.aljaz.spendingtracker.data.local.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object: all the ways the app reads and writes transactions.
 * Room generates the implementation for us.
 */
@Dao
interface TransactionDao {

    /**
     * Inserts a transaction.
     *
     * If a row with the same dedupeHash already exists, the insert is
     * ignored instead of crashing.
     *
     * @return the new row id, or -1 if it was ignored as a duplicate.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(transaction: TransactionEntity): Long

    /** Saves changes to an existing transaction (matched by id). */
    @Update
    suspend fun update(transaction: TransactionEntity)

    /** Permanently removes a transaction. */
    @Delete
    suspend fun delete(transaction: TransactionEntity)

    /** Returns one transaction by id, or null if it doesn't exist. */
    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getById(id: Long): TransactionEntity?

    /**
     * Streams all transactions within a time range, newest first.
     * The list updates automatically whenever the table changes.
     *
     * @param startMillis start of the range (inclusive), UTC epoch millis
     * @param endMillis end of the range (exclusive), UTC epoch millis
     */
    @Query(
        "SELECT * FROM transactions " +
                "WHERE timestamp >= :startMillis AND timestamp < :endMillis " +
                "ORDER BY timestamp DESC"
    )
    fun observeBetween(startMillis: Long, endMillis: Long): Flow<List<TransactionEntity>>

    /** Every transaction, oldest first. Used for exports and backups. */
    @Query("SELECT * FROM transactions ORDER BY timestamp ASC")
    suspend fun getAll(): List<TransactionEntity>


    /**
     * Makes every transaction in a category uncategorized.
     * Used before deleting that category.
     */
    @Query("UPDATE transactions SET categoryId = NULL WHERE categoryId = :categoryId")
    suspend fun clearCategory(categoryId: Long)


    /** Streams automatic transactions not yet confirmed by the user, newest first. */
    @Query("SELECT * FROM transactions WHERE reviewed = 0 ORDER BY timestamp DESC")
    fun observeUnreviewed(): Flow<List<TransactionEntity>>

    /** Marks the given transactions as confirmed. */
    @Query("UPDATE transactions SET reviewed = 1 WHERE id IN (:ids)")
    suspend fun markReviewed(ids: List<Long>)


    /** Streams every transaction, newest first; updates automatically. */
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<TransactionEntity>>


    /**
     * Streams income minus expenses of everything before [before]
     * (UTC epoch millis), in minor units. Used for the carry-over balance.
     */
    @Query(
        "SELECT COALESCE(SUM(CASE WHEN type = 'INCOME' THEN amountMinor ELSE -amountMinor END), 0) " +
                "FROM transactions WHERE timestamp < :before"
    )
    fun observeBalanceBefore(before: Long): Flow<Long>


    /** Unlinks every entry from a series; used when the series is stopped. */
    @Query("UPDATE transactions SET recurringId = NULL WHERE recurringId = :recurringId")
    suspend fun clearRecurring(recurringId: Long)
}