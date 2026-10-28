package com.aljaz.spendingtracker.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.aljaz.spendingtracker.data.local.entity.SettlementEntity
import kotlinx.coroutines.flow.Flow

/** Reads and writes the local mirror of settle-up payments. */
@Dao
interface SettlementDao {

    /** Inserts the payment, or replaces the row with the same uuid. */
    @Upsert
    suspend fun upsert(settlement: SettlementEntity)

    @Query("DELETE FROM settlements WHERE uuid = :uuid")
    suspend fun deleteByUuid(uuid: String)

    /** The uuids of every mirrored payment; used to find ones the server no longer has. */
    @Query("SELECT uuid FROM settlements")
    suspend fun getAllUuids(): List<String>

    /** Streams every payment, newest first. */
    @Query("SELECT * FROM settlements ORDER BY date DESC")
    fun observeAll(): Flow<List<SettlementEntity>>

    /** Empties the mirror, e.g. on logout. */
    @Query("DELETE FROM settlements")
    suspend fun clear()
}