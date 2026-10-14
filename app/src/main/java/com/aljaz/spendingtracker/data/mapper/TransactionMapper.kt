package com.aljaz.spendingtracker.data.mapper

import com.aljaz.spendingtracker.data.local.entity.TransactionEntity
import com.aljaz.spendingtracker.domain.model.Transaction

/** Converts a database row into the domain model. */
fun TransactionEntity.toDomain(): Transaction = Transaction(
    id = id,
    type = type,
    amountMinor = amountMinor,
    currency = currency,
    categoryId = categoryId,
    title = title,
    note = note,
    timestamp = timestamp,
    source = source,
    rawText = rawText,
    dedupeHash = dedupeHash,
    reviewed = reviewed
)

/** Converts the domain model into a database row. */
fun Transaction.toEntity(): TransactionEntity = TransactionEntity(
    id = id,
    type = type,
    amountMinor = amountMinor,
    currency = currency,
    categoryId = categoryId,
    title = title,
    note = note,
    timestamp = timestamp,
    source = source,
    rawText = rawText,
    dedupeHash = dedupeHash,
    reviewed = reviewed
)