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
}