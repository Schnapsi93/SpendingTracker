package com.aljaz.spendingtracker.domain.usecase

import com.aljaz.spendingtracker.domain.repository.TransactionRepository
import javax.inject.Inject

/** Marks transactions as reviewed, removing them from the review queue. */
class ConfirmTransactionsUseCase @Inject constructor(
    private val repository: TransactionRepository
) {

    suspend operator fun invoke(ids: List<Long>) = repository.markReviewed(ids)
}