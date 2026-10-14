package com.aljaz.spendingtracker.domain.usecase

import com.aljaz.spendingtracker.core.AppResult
import com.aljaz.spendingtracker.domain.model.Transaction
import com.aljaz.spendingtracker.domain.repository.TransactionRepository
import javax.inject.Inject

/**
 * Validates and saves changes to an existing transaction.
 */
class UpdateTransactionUseCase @Inject constructor(
    private val repository: TransactionRepository
) {

    /**
     * @param transaction the edited transaction (must have a real id)
     * @return [AppResult.Success] when saved, or [AppResult.Error] with a reason
     */
    suspend operator fun invoke(transaction: Transaction): AppResult<Unit> {
        if (transaction.id == 0L) {
            return AppResult.Error("Cannot update a transaction that was never saved.")
        }

        val cleaned = when (val check = transaction.validateAndClean()) {
            is AppResult.Error -> return check
            is AppResult.Success -> check.data
        }

        repository.update(cleaned)
        return AppResult.Success(Unit)
    }
}