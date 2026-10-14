package com.aljaz.spendingtracker.domain.usecase

import com.aljaz.spendingtracker.domain.model.Transaction
import com.aljaz.spendingtracker.domain.repository.TransactionRepository
import javax.inject.Inject

/** Permanently deletes a transaction. */
class DeleteTransactionUseCase @Inject constructor(
    private val repository: TransactionRepository
) {

    suspend operator fun invoke(transaction: Transaction) =
        repository.delete(transaction)
}