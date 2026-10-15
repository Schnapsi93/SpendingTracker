package com.aljaz.spendingtracker.data.mapper

import com.aljaz.spendingtracker.data.local.entity.UnrecognizedMessageEntity
import com.aljaz.spendingtracker.domain.model.UnrecognizedMessage

/** Converts a database row into the domain model. */
fun UnrecognizedMessageEntity.toDomain(): UnrecognizedMessage = UnrecognizedMessage(
    id = id,
    sender = sender,
    body = body,
    receivedAt = receivedAt,
    source = source
)

/** Converts the domain model into a database row. */
fun UnrecognizedMessage.toEntity(): UnrecognizedMessageEntity = UnrecognizedMessageEntity(
    id = id,
    sender = sender,
    body = body,
    receivedAt = receivedAt,
    source = source
)