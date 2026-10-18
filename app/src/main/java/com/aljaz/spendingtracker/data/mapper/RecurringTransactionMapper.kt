package com.aljaz.spendingtracker.data.mapper

import com.aljaz.spendingtracker.data.local.entity.RecurringTransactionEntity
import com.aljaz.spendingtracker.domain.model.RecurringTransaction

/** Converts a database row into the domain model. */
fun RecurringTransactionEntity.toDomain(): RecurringTransaction = RecurringTransaction(
    id = id,
    type = type,
    amountMinor = amountMinor,
    currency = currency,
    categoryId = categoryId,
    title = title,
    note = note,
    frequency = frequency,
    startTimestamp = startTimestamp,
    generatedCount = generatedCount,
    createdAt = createdAt
)

/** Converts the domain model into a database row. */
fun RecurringTransaction.toEntity(): RecurringTransactionEntity = RecurringTransactionEntity(
    id = id,
    type = type,
    amountMinor = amountMinor,
    currency = currency,
    categoryId = categoryId,
    title = title,
    note = note,
    frequency = frequency,
    startTimestamp = startTimestamp,
    generatedCount = generatedCount,
    createdAt = createdAt
)