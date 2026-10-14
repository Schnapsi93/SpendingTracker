package com.aljaz.spendingtracker.domain.usecase

import com.aljaz.spendingtracker.domain.model.Transaction
import com.aljaz.spendingtracker.domain.repository.TransactionRepository
import javax.inject.Inject

/** Loads a single transaction, e.g. to fill the edit form. */
class GetTransactionUseCase @Inject constructor(
    private val repository: TransactionRepository
) {

    /** @return the transaction, or null if it no longer exists */
    suspend operator fun invoke(id: Long): Transaction? = repository.getById(id)
}