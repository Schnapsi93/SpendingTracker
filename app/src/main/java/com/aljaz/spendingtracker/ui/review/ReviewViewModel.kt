package com.aljaz.spendingtracker.ui.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aljaz.spendingtracker.domain.model.Transaction
import com.aljaz.spendingtracker.domain.usecase.ConfirmTransactionsUseCase
import com.aljaz.spendingtracker.domain.usecase.ObserveCategoriesUseCase
import com.aljaz.spendingtracker.domain.usecase.ObserveUnreviewedTransactionsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Logic behind the Review screen: lists unreviewed automatic
 * transactions and confirms them.
 */
@HiltViewModel
class ReviewViewModel @Inject constructor(
    observeUnreviewed: ObserveUnreviewedTransactionsUseCase,
    observeCategories: ObserveCategoriesUseCase,
    private val confirmTransactions: ConfirmTransactionsUseCase
) : ViewModel() {

    /** Updates by itself when a transaction is confirmed, edited or deleted. */
    val uiState: StateFlow<ReviewUiState> = combine(
        observeUnreviewed(),
        observeCategories()
    ) { transactions, categories ->
        ReviewUiState(
            transactions = transactions,
            categoriesById = categories.associateBy { it.id },
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ReviewUiState()
    )

    /** Confirms one transaction as it is. */
    fun onConfirm(transaction: Transaction) {
        viewModelScope.launch { confirmTransactions(listOf(transaction.id)) }
    }

    /** Confirms every transaction currently in the queue. */
    fun onConfirmAll() {
        val ids = uiState.value.transactions.map { it.id }
        viewModelScope.launch { confirmTransactions(ids) }
    }
}