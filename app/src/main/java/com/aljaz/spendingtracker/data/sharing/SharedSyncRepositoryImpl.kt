package com.aljaz.spendingtracker.data.sharing

import android.content.Context
import com.aljaz.spendingtracker.core.AppResult
import com.aljaz.spendingtracker.data.local.dao.CategoryDao
import com.aljaz.spendingtracker.data.local.dao.PendingDeletionDao
import com.aljaz.spendingtracker.data.local.dao.SharedExpenseDao
import com.aljaz.spendingtracker.data.local.dao.TransactionDao
import com.aljaz.spendingtracker.data.local.entity.PendingDeletionEntity
import com.aljaz.spendingtracker.data.local.entity.SharedExpenseEntity
import com.aljaz.spendingtracker.data.local.entity.TransactionEntity
import com.aljaz.spendingtracker.domain.model.SyncState
import com.aljaz.spendingtracker.domain.sharing.SharedAccount
import com.aljaz.spendingtracker.domain.sharing.SharedSyncRepository
import com.aljaz.spendingtracker.domain.sharing.SyncStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The sync engine. One run does, in this order:
 *
 * 1. Log in again with the saved password (fresh token every run).
 * 2. Upload this user's confirmed PENDING transactions: shared ones are
 *    created or updated on the server; ones that are no longer shared are
 *    marked deleted there.
 * 3. Upload deletions (tombstones) of transactions deleted on this phone.
 * 4. Download every change in the household since the last run into the
 *    local mirror ("shared_expenses").
 *
 * Only the payer may change an expense on the server, so the two phones
 * can never overwrite each other's data.
 */
@Singleton
class SharedSyncRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val client: PocketBaseClient,
    private val accountStore: SharedAccountStore,
    private val statusStore: SyncStatusStore,
    private val transactionDao: TransactionDao,
    private val categoryDao: CategoryDao,
    private val sharedExpenseDao: SharedExpenseDao,
    private val pendingDeletionDao: PendingDeletionDao
) : SharedSyncRepository {

    /** Makes sure only one sync runs at a time (button and background job). */
    private val mutex = Mutex()

    override val status: StateFlow<SyncStatus> = statusStore.status

    override fun requestSync() {
        if (accountStore.account.value != null) SyncScheduler.requestNow(context)
    }

    override suspend fun syncNow(): AppResult<Unit> = mutex.withLock {
        val account = accountStore.account.value ?: return@withLock AppResult.Success(Unit)

        statusStore.setSyncing(true)
        try {
            val result = withContext(Dispatchers.IO) { runSync(account) }
            when (result) {
                is AppResult.Success -> statusStore.recordSuccess(System.currentTimeMillis())
                is AppResult.Error -> statusStore.recordError(result.message)
            }
            result
        } finally {
            statusStore.setSyncing(false)
        }
    }

    override suspend fun rememberDeletion(uuid: String) = withContext(Dispatchers.IO) {
        pendingDeletionDao.insert(
            PendingDeletionEntity(uuid = uuid, createdAt = System.currentTimeMillis())
        )
    }

    @OptIn(FlowPreview::class)
    override fun startAutoSync(scope: CoroutineScope) {
        SyncScheduler.schedulePeriodic(context)
        requestSync() // download the partner's news on every app start

        // Sync shortly after anything is waiting to be uploaded.
        scope.launch {
            combine(
                accountStore.account,
                transactionDao.observePendingSyncCount(),
                pendingDeletionDao.observeCount()
            ) { account, pending, deletions ->
                if (account == null) 0 else pending + deletions
            }
                .distinctUntilChanged()
                .debounce(AUTO_SYNC_DELAY_MILLIS) // several quick edits → one sync
                .filter { it > 0 }
                .collect { requestSync() }
        }
    }

    override suspend fun resetDownloads() = withContext(Dispatchers.IO) {
        sharedExpenseDao.clear()
        statusStore.resetCursor()
    }

    override suspend fun clearLocalData() = withContext(Dispatchers.IO) {
        SyncScheduler.cancel(context)
        sharedExpenseDao.clear()
        pendingDeletionDao.clear()
        statusStore.clear()
    }

    // ---------- One sync run ----------

    private suspend fun runSync(account: SharedAccount): AppResult<Unit> {
        val token = when (val result = logIn(account)) {
            is AppResult.Error -> return result
            is AppResult.Success -> result.data
        }

        val upload = uploadOwnExpenses(account, token)
        if (upload is AppResult.Error) return upload

        val deletions = uploadDeletions(account, token)
        if (deletions is AppResult.Error) return deletions

        return downloadChanges(account, token)
    }

    /** Step 1: logs in with the saved password and returns a fresh token. */
    private suspend fun logIn(account: SharedAccount): AppResult<String> {
        val password = accountStore.password()
            ?: return AppResult.Error(
                "The saved login can't be read. Log out and in again under Shared account."
            )
        return when (val result = client.authWithPassword(account.serverUrl, account.email, password)) {
            is AppResult.Error -> result
            is AppResult.Success -> {
                accountStore.updateToken(result.data.token)
                AppResult.Success(result.data.token)
            }
        }
    }

    /** Step 2: uploads confirmed PENDING transactions. */
    private suspend fun uploadOwnExpenses(account: SharedAccount, token: String): AppResult<Unit> {
        for (row in transactionDao.getPendingSync()) {
            val remoteId = when (val found = client.findExpenseId(account.serverUrl, token, row.uuid)) {
                is AppResult.Error -> return found
                is AppResult.Success -> found.data
            }

            val result = when {
                // Shared: create it, or update the existing server copy.
                row.isShared -> {
                    val upload = row.toUpload(account)
                    if (remoteId == null) {
                        client.createExpense(account.serverUrl, token, upload)
                    } else {
                        client.updateExpense(account.serverUrl, token, remoteId, upload)
                    }
                }
                // No longer shared, but it is on the server: remove it there.
                remoteId != null ->
                    client.markExpenseDeleted(account.serverUrl, token, remoteId, row.updatedAt)
                // No longer shared and never uploaded: nothing to do.
                else -> AppResult.Success(Unit)
            }
            if (result is AppResult.Error) return result

            val newState = if (row.isShared) SyncState.SYNCED else SyncState.LOCAL_ONLY
            transactionDao.markSyncState(row.id, row.updatedAt, newState.name)
        }
        return AppResult.Success(Unit)
    }

    /** Step 3: marks expenses deleted on this phone as deleted on the server. */
    private suspend fun uploadDeletions(account: SharedAccount, token: String): AppResult<Unit> {
        for (deletion in pendingDeletionDao.getAll()) {
            val remoteId = when (val found = client.findExpenseId(account.serverUrl, token, deletion.uuid)) {
                is AppResult.Error -> return found
                is AppResult.Success -> found.data
            }
            if (remoteId != null) {
                val result = client.markExpenseDeleted(
                    account.serverUrl, token, remoteId, deletion.createdAt
                )
                if (result is AppResult.Error) return result
            }
            // Done, or it was never uploaded: the tombstone is no longer needed.
            pendingDeletionDao.delete(deletion.uuid)
        }
        return AppResult.Success(Unit)
    }

    /** Step 4: downloads every change since the last run into the mirror. */
    private suspend fun downloadChanges(account: SharedAccount, token: String): AppResult<Unit> {
        val since = statusStore.cursor()
        var newest = since
        var page = 1

        do {
            val result = client.fetchExpensesChangedSince(
                account.serverUrl, token, account.householdId, since, page
            )
            val data = when (result) {
                is AppResult.Error -> return result
                is AppResult.Success -> result.data
            }

            for (remote in data.items) {
                if (remote.deleted) {
                    sharedExpenseDao.deleteByUuid(remote.uuid)
                } else {
                    sharedExpenseDao.upsert(remote.toEntity())
                }
                newest = remote.updated // sorted oldest first, so the last one is newest
            }
            page++
        } while (page <= data.totalPages)

        // Saved only after everything arrived, so an interrupted run starts over safely.
        newest?.let { statusStore.saveCursor(it) }
        return AppResult.Success(Unit)
    }

    // ---------- Conversions ----------

    /** A local transaction as it is uploaded; the category travels by name. */
    private suspend fun TransactionEntity.toUpload(account: SharedAccount): ExpenseUpload {
        val categoryName = categoryId?.let { categoryDao.getById(it)?.name } ?: "Other"
        return ExpenseUpload(
            uuid = uuid,
            householdId = account.householdId,
            paidBy = account.userId,
            amountMinor = amountMinor,
            currency = currency,
            date = timestamp,
            category = categoryName,
            payerSharePercent = payerSharePercent ?: DEFAULT_SPLIT_PERCENT,
            note = note,
            clientUpdatedAt = updatedAt
        )
    }

    private fun RemoteExpense.toEntity(): SharedExpenseEntity = SharedExpenseEntity(
        uuid = uuid,
        remoteId = id,
        householdId = householdId,
        paidBy = paidBy,
        amountMinor = amountMinor,
        currency = currency,
        date = date,
        category = category,
        payerSharePercent = payerSharePercent,
        note = note,
        serverUpdated = updated
    )

    private companion object {
        /** Waits this long after the last change before syncing. */
        const val AUTO_SYNC_DELAY_MILLIS = 2_000L
        const val DEFAULT_SPLIT_PERCENT = 50
    }
}