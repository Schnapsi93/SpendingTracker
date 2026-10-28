package com.aljaz.spendingtracker.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.aljaz.spendingtracker.data.local.entity.PendingDeletionEntity
import kotlinx.coroutines.flow.Flow

/** Tombstones of deleted transactions that still have to be removed on the server. */
@Dao
interface PendingDeletionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(deletion: PendingDeletionEntity)

    @Query("SELECT * FROM pending_deletions")
    suspend fun getAll(): List<PendingDeletionEntity>

    @Query("DELETE FROM pending_deletions WHERE uuid = :uuid")
    suspend fun delete(uuid: String)

    /** How many deletions are waiting; the sync starts when this goes above 0. */
    @Query("SELECT COUNT(*) FROM pending_deletions")
    fun observeCount(): Flow<Int>

    @Query("DELETE FROM pending_deletions")
    suspend fun clear()
}