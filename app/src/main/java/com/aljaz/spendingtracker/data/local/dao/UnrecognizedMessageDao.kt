package com.aljaz.spendingtracker.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.aljaz.spendingtracker.data.local.entity.UnrecognizedMessageEntity
import kotlinx.coroutines.flow.Flow

/** Reads and writes unrecognized bank messages. */
@Dao
interface UnrecognizedMessageDao {

    /** Ignored if the same sender + text is already stored. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(message: UnrecognizedMessageEntity)

    @Query("DELETE FROM unrecognized_messages WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM unrecognized_messages ORDER BY receivedAt DESC")
    fun observeAll(): Flow<List<UnrecognizedMessageEntity>>
}