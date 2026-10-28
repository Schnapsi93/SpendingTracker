package com.aljaz.spendingtracker.domain.usecase

import com.aljaz.spendingtracker.domain.model.SyncState
import com.aljaz.spendingtracker.domain.model.Transaction
import com.aljaz.spendingtracker.domain.repository.TransactionRepository
import com.aljaz.spendingtracker.domain.sharing.SharedSyncRepository
import javax.inject.Inject

/** Permanently deletes a transaction (and, if it was shared, its server copy). */
class DeleteTransactionUseCase @Inject constructor(
    private val repository: TransactionRepository,
    private val sync: SharedSyncRepository
) {

    suspend operator fun invoke(transaction: Transaction) {
        repository.delete(transaction)

        // It may be on the server: leave a tombstone so the sync removes it there too.
        // (If it was never uploaded, the sync simply finds nothing and drops the note.)
        if (transaction.syncState != SyncState.LOCAL_ONLY) {
            sync.rememberDeletion(transaction.uuid)
        }
    }
}