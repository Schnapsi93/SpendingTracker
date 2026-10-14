package com.aljaz.spendingtracker.domain.usecase

import com.aljaz.spendingtracker.core.AppResult
import com.aljaz.spendingtracker.domain.model.Transaction
import com.aljaz.spendingtracker.domain.repository.TransactionRepository
import javax.inject.Inject

/**
 * Validates and saves a new transaction.
 *
 * Every entry point (manual form, SMS, CSV import) goes through here,
 * so they all follow the same rules.
 */
class AddTransactionUseCase @Inject constructor(
    private val repository: TransactionRepository
) {

    /**
     * @param transaction the transaction to save (id should be 0)
     * @return [AppResult.Success] with the new id, or [AppResult.Error]
     *         with a message the user can read
     */
    suspend operator fun invoke(transaction: Transaction): AppResult<Long> {
        val cleaned = when (val check = transaction.validateAndClean()) {
            is AppResult.Error -> return check
            is AppResult.Success -> check.data
        }

        val newId = repository.add(cleaned)
            ?: return AppResult.Error("This transaction already exists.")
        return AppResult.Success(newId)
    }
}