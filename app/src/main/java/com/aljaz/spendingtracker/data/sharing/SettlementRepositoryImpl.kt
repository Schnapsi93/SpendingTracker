package com.aljaz.spendingtracker.data.sharing

import com.aljaz.spendingtracker.core.AppResult
import com.aljaz.spendingtracker.data.local.dao.SettlementDao
import com.aljaz.spendingtracker.data.mapper.toDomain
import com.aljaz.spendingtracker.data.mapper.toEntity
import com.aljaz.spendingtracker.domain.sharing.Settlement
import com.aljaz.spendingtracker.domain.sharing.SettlementRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [SettlementRepository] that sends changes straight to PocketBase and
 * updates the local mirror right away, so the balance changes immediately.
 */
@Singleton
class SettlementRepositoryImpl @Inject constructor(
    private val client: PocketBaseClient,
    private val session: PocketBaseSession,
    private val accountStore: SharedAccountStore,
    private val dao: SettlementDao
) : SettlementRepository {

    override fun observeAll(): Flow<List<Settlement>> =
        dao.observeAll().map { rows -> rows.map { it.toDomain() } }

    override suspend fun record(
        fromUser: String,
        toUser: String,
        amountMinor: Long,
        note: String?
    ): AppResult<Unit> {
        val account = accountStore.account.value
            ?: return AppResult.Error("Log in to the shared account first.")
        if (amountMinor <= 0) return AppResult.Error("Amount must be greater than zero.")

        val token = when (val result = session.freshToken(account)) {
            is AppResult.Error -> return result
            is AppResult.Success -> result.data
        }

        val upload = SettlementUpload(
            uuid = UUID.randomUUID().toString(),
            householdId = account.householdId,
            fromUser = fromUser,
            toUser = toUser,
            amountMinor = amountMinor,
            date = System.currentTimeMillis(),
            note = note?.trim()?.ifEmpty { null }
        )

        return when (val result = client.createSettlement(account.serverUrl, token, upload)) {
            is AppResult.Error -> result
            is AppResult.Success -> {
                // Show it at once; the next sync downloads the same record again, harmlessly.
                withContext(Dispatchers.IO) { dao.upsert(result.data.toEntity()) }
                AppResult.Success(Unit)
            }
        }
    }

    override suspend fun delete(settlement: Settlement): AppResult<Unit> {
        val account = accountStore.account.value
            ?: return AppResult.Error("Log in to the shared account first.")

        val token = when (val result = session.freshToken(account)) {
            is AppResult.Error -> return result
            is AppResult.Success -> result.data
        }

        val remoteId = when (
            val result = client.findSettlementId(account.serverUrl, token, settlement.uuid)
        ) {
            is AppResult.Error -> return result
            is AppResult.Success -> result.data
        }

        if (remoteId != null) {
            val result = client.markSettlementDeleted(account.serverUrl, token, remoteId)
            if (result is AppResult.Error) return result
        }

        withContext(Dispatchers.IO) { dao.deleteByUuid(settlement.uuid) }
        return AppResult.Success(Unit)
    }
}