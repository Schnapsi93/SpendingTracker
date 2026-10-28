package com.aljaz.spendingtracker.domain.usecase

import com.aljaz.spendingtracker.domain.repository.CategoryRepository
import com.aljaz.spendingtracker.domain.repository.RecurringTransactionRepository
import com.aljaz.spendingtracker.domain.repository.TransactionRepository
import java.time.ZoneId
import javax.inject.Inject

/**
 * Creates every recurring occurrence whose time has come.
 *
 * Safe to run any number of times: each occurrence has its own dedupe
 * hash, and the template remembers how far it got. If the phone was off
 * for a while, all missed occurrences are caught up in one run.
 *
 * Entries in a split category are shared automatically, like a manual
 * entry in that category would be.
 */
class GenerateDueRecurringTransactionsUseCase @Inject constructor(
    private val recurringRepository: RecurringTransactionRepository,
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository
) {

    /**
     * @param now the current moment, UTC epoch millis (a parameter so tests can fix it)
     * @return how many transactions were created
     */
    suspend operator fun invoke(now: Long = System.currentTimeMillis()): Int {
        val zone = ZoneId.systemDefault()
        var created = 0

        for (template in recurringRepository.getAll()) {
            var count = template.generatedCount
            val category = template.categoryId?.let { categoryRepository.getById(it) }

            // Safety cap, so a broken start date can never loop for ages.
            var guard = 0
            while (template.occurrenceAt(count, zone) <= now && guard < MAX_PER_RUN) {
                val transaction = template.toTransaction(count, zone)
                    .withSplitDefaults(category)
                    .withSyncStateForSave()

                // A null result means it already exists; just move on.
                if (transactionRepository.add(transaction) != null) {
                    created++
                }
                count++
                guard++
            }

            if (count != template.generatedCount) {
                recurringRepository.update(template.copy(generatedCount = count))
            }
        }
        return created
    }

    private companion object {
        /** About 10 years of weekly entries. */
        const val MAX_PER_RUN = 520
    }
}