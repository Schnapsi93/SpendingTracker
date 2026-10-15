package com.aljaz.spendingtracker.domain.usecase

import com.aljaz.spendingtracker.domain.model.Transaction
import com.aljaz.spendingtracker.domain.model.TransactionFilter
import com.aljaz.spendingtracker.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

/**
 * Streams all transactions matching a filter, newest first.
 * Emits again whenever the filter or the data changes.
 */
class SearchTransactionsUseCase @Inject constructor(
    private val repository: TransactionRepository
) {

    operator fun invoke(filter: Flow<TransactionFilter>): Flow<List<Transaction>> =
        combine(repository.observeAll(), filter) { transactions, currentFilter ->
            transactions.filter(currentFilter::matches)
        }
}