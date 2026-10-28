package com.aljaz.spendingtracker.data.mapper

import com.aljaz.spendingtracker.data.local.entity.SettlementEntity
import com.aljaz.spendingtracker.data.sharing.RemoteSettlement
import com.aljaz.spendingtracker.domain.sharing.Settlement

/** Converts a payment from the server into a mirror row. */
fun RemoteSettlement.toEntity(): SettlementEntity = SettlementEntity(
    uuid = uuid,
    remoteId = id,
    householdId = householdId,
    fromUser = fromUser,
    toUser = toUser,
    amountMinor = amountMinor,
    date = date,
    note = note,
    serverUpdated = updated
)

/** Converts a mirror row into the domain model (server-only fields are left out). */
fun SettlementEntity.toDomain(): Settlement = Settlement(
    uuid = uuid,
    fromUser = fromUser,
    toUser = toUser,
    amountMinor = amountMinor,
    date = date,
    note = note
)