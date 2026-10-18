package com.aljaz.spendingtracker.data.repository

import com.aljaz.spendingtracker.data.local.dao.RecurringTransactionDao
import com.aljaz.spendingtracker.data.mapper.toDomain
import com.aljaz.spendingtracker.data.mapper.toEntity
import com.aljaz.spendingtracker.domain.model.RecurringTransaction
import com.aljaz.spendingtracker.domain.repository.RecurringTransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/** Room-backed implementation of [RecurringTransactionRepository]. */
class RecurringTransactionRepositoryImpl @Inject constructor(
    private val dao: RecurringTransactionDao
) : RecurringTransactionRepository {

    override suspend fun add(recurring: RecurringTransaction): Long =
        dao.insert(recurring.toEntity())

    override suspend fun update(recurring: RecurringTransaction) =
        dao.update(recurring.toEntity())

    override suspend fun delete(recurring: RecurringTransaction) =
        dao.delete(recurring.toEntity())

    override suspend fun getAll(): List<RecurringTransaction> =
        dao.getAll().map { it.toDomain() }

    override fun observeAll(): Flow<List<RecurringTransaction>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }
}