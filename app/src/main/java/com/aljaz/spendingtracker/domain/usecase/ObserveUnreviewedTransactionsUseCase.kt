package com.aljaz.spendingtracker.domain.usecase

import com.aljaz.spendingtracker.domain.model.Transaction
import com.aljaz.spendingtracker.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** Streams the automatic transactions waiting for the user's confirmation. */
class ObserveUnreviewedTransactionsUseCase @Inject constructor(
    private val repository: TransactionRepository
) {

    operator fun invoke(): Flow<List<Transaction>> = repository.observeUnreviewed()
}