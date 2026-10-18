package com.aljaz.spendingtracker.domain.usecase

import com.aljaz.spendingtracker.core.AppResult
import com.aljaz.spendingtracker.domain.model.RecurringTransaction
import com.aljaz.spendingtracker.domain.model.RepeatFrequency
import com.aljaz.spendingtracker.domain.model.Transaction
import com.aljaz.spendingtracker.domain.repository.RecurringTransactionRepository
import javax.inject.Inject

/**
 * Saves a transaction from the form and starts repeating it.
 *
 * The transaction itself is the first occurrence. The template then
 * creates the following ones in the background. If the chosen date lies
 * in the past, missed occurrences up to today are created right away.
 */
class AddRecurringTransactionUseCase @Inject constructor(
    private val addTransaction: AddTransactionUseCase,
    private val recurringRepository: RecurringTransactionRepository,
    private val generateDue: GenerateDueRecurringTransactionsUseCase
) {

    /**
     * @param first the transaction as entered in the form (id should be 0)
     * @param frequency how often it repeats
     * @return [AppResult.Success] with the new transaction's id, or
     *         [AppResult.Error] with a message the user can read
     */
    suspend operator fun invoke(
        first: Transaction,
        frequency: RepeatFrequency
    ): AppResult<Long> {
        // Same validation as any other transaction; stop if it fails.
        val transactionId = when (val result = addTransaction(first)) {
            is AppResult.Error -> return result
            is AppResult.Success -> result.data
        }

        // Use the cleaned values (e.g. default title) for every repetition.
        val cleaned = (first.validateAndClean() as? AppResult.Success)?.data ?: first

        recurringRepository.add(
            RecurringTransaction(
                type = cleaned.type,
                amountMinor = cleaned.amountMinor,
                currency = cleaned.currency,
                categoryId = cleaned.categoryId,
                title = cleaned.title,
                note = cleaned.note,
                frequency = frequency,
                startTimestamp = cleaned.timestamp,
                generatedCount = 1 // occurrence 0 is the transaction saved above
            )
        )

        generateDue() // catch up if the start date was long ago
        return AppResult.Success(transactionId)
    }
}