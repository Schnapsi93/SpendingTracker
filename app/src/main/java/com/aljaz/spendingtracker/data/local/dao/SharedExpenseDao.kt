package com.aljaz.spendingtracker.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.aljaz.spendingtracker.data.local.entity.SharedExpenseEntity
import kotlinx.coroutines.flow.Flow

/** Reads and writes the local mirror of the server's shared expenses. */
@Dao
interface SharedExpenseDao {

    /** Inserts the expense, or replaces the row with the same uuid. */
    @Upsert
    suspend fun upsert(expense: SharedExpenseEntity)

    /** Removes one expense, e.g. after it was deleted on the server. */
    @Query("DELETE FROM shared_expenses WHERE uuid = :uuid")
    suspend fun deleteByUuid(uuid: String)

    /**
     * Streams the shared expenses within a time range, newest first.
     *
     * @param startMillis range start (inclusive), UTC epoch millis
     * @param endMillis range end (exclusive), UTC epoch millis
     */
    @Query(
        "SELECT * FROM shared_expenses " +
                "WHERE date >= :startMillis AND date < :endMillis " +
                "ORDER BY date DESC"
    )
    fun observeBetween(startMillis: Long, endMillis: Long): Flow<List<SharedExpenseEntity>>

    /** Empties the mirror, e.g. on logout. */
    @Query("DELETE FROM shared_expenses")
    suspend fun clear()
}