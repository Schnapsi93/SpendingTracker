package com.aljaz.spendingtracker.data.repository

import com.aljaz.spendingtracker.data.local.dao.TransactionDao
import com.aljaz.spendingtracker.data.mapper.toDomain
import com.aljaz.spendingtracker.data.mapper.toEntity
import com.aljaz.spendingtracker.domain.model.Transaction
import com.aljaz.spendingtracker.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Room-backed implementation of [TransactionRepository].
 *
 * Its only job is to call the DAO and convert between database
 * rows and domain models.
 */
class TransactionRepositoryImpl @Inject constructor(
    private val dao: TransactionDao
) : TransactionRepository {

    override suspend fun add(transaction: Transaction): Long? {
        val rowId = dao.insert(transaction.toEntity())
        // The DAO returns -1 when the insert was ignored as a duplicate.
        return if (rowId == -1L) null else rowId
    }

    override suspend fun update(transaction: Transaction) =
        dao.update(transaction.toEntity())

    override suspend fun delete(transaction: Transaction) =
        dao.delete(transaction.toEntity())

    override suspend fun getById(id: Long): Transaction? =
        dao.getById(id)?.toDomain()

    override fun observeBetween(
        startMillis: Long,
        endMillis: Long
    ): Flow<List<Transaction>> =
        dao.observeBetween(startMillis, endMillis)
            .map { entities -> entities.map { it.toDomain() } }
}