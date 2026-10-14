package com.aljaz.spendingtracker.domain.usecase

import com.aljaz.spendingtracker.core.AppResult
import com.aljaz.spendingtracker.domain.model.Transaction
import com.aljaz.spendingtracker.domain.model.TransactionType
import com.aljaz.spendingtracker.domain.repository.TransactionRepository
import javax.inject.Inject

/**
 * Validates and saves a new transaction.
 *
 * All rules about what makes a transaction acceptable live here,
 * so every entry point (manual form, SMS, CSV import) behaves the same.
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
        if (transaction.amountMinor <= 0) {
            return AppResult.Error("Amount must be greater than zero.")
        }

        // A blank title falls back to the type name, so quick entries work.
        val cleaned = transaction.copy(
            title = transaction.title.trim().ifEmpty {
                when (transaction.type) {
                    TransactionType.INCOME -> "Income"
                    TransactionType.EXPENSE -> "Expense"
                }
            },
            note = transaction.note?.trim()?.ifEmpty { null }
        )

        val newId = repository.add(cleaned)
            ?: return AppResult.Error("This transaction already exists.")
        return AppResult.Success(newId)
    }
}