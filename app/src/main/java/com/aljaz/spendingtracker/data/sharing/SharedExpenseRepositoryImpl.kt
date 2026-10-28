package com.aljaz.spendingtracker.data.sharing

import com.aljaz.spendingtracker.data.local.dao.SharedExpenseDao
import com.aljaz.spendingtracker.data.local.entity.SharedExpenseEntity
import com.aljaz.spendingtracker.domain.sharing.SharedExpense
import com.aljaz.spendingtracker.domain.sharing.SharedExpenseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** [SharedExpenseRepository] backed by the local mirror table. */
@Singleton
class SharedExpenseRepositoryImpl @Inject constructor(
    private val dao: SharedExpenseDao
) : SharedExpenseRepository {

    override fun observeBetween(startMillis: Long, endMillis: Long): Flow<List<SharedExpense>> =
        dao.observeBetween(startMillis, endMillis).map { rows -> rows.map { it.toDomain() } }

    override fun observeAll(): Flow<List<SharedExpense>> =
        dao.observeAll().map { rows -> rows.map { it.toDomain() } }

    /** Converts a mirror row into the domain model (server-only fields are left out). */
    private fun SharedExpenseEntity.toDomain(): SharedExpense = SharedExpense(
        uuid = uuid,
        paidBy = paidBy,
        amountMinor = amountMinor,
        currency = currency,
        date = date,
        category = category,
        payerSharePercent = payerSharePercent,
        note = note
    )
}