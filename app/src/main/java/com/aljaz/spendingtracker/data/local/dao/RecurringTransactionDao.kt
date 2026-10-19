package com.aljaz.spendingtracker.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.aljaz.spendingtracker.data.local.entity.RecurringTransactionEntity
import kotlinx.coroutines.flow.Flow

/** All the ways the app reads and writes recurring transaction templates. */
@Dao
interface RecurringTransactionDao {

    /** Inserts a template and returns its new id. */
    @Insert
    suspend fun insert(recurring: RecurringTransactionEntity): Long

    @Update
    suspend fun update(recurring: RecurringTransactionEntity)

    @Delete
    suspend fun delete(recurring: RecurringTransactionEntity)

    @Query("SELECT * FROM recurring_transactions")
    suspend fun getAll(): List<RecurringTransactionEntity>

    @Query("SELECT * FROM recurring_transactions ORDER BY title COLLATE NOCASE")
    fun observeAll(): Flow<List<RecurringTransactionEntity>>

    /** Makes every template in a category uncategorized; used before deleting it. */
    @Query("UPDATE recurring_transactions SET categoryId = NULL WHERE categoryId = :categoryId")
    suspend fun clearCategory(categoryId: Long)

    @Query("SELECT * FROM recurring_transactions WHERE id = :id")
    suspend fun getById(id: Long): RecurringTransactionEntity?
}