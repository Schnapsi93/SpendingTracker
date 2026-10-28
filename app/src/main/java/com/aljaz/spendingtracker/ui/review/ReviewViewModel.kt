package com.aljaz.spendingtracker.ui.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aljaz.spendingtracker.automation.BankMessageProcessor
import com.aljaz.spendingtracker.automation.BankMessageResult
import com.aljaz.spendingtracker.domain.model.Transaction
import com.aljaz.spendingtracker.domain.model.UnrecognizedMessage
import com.aljaz.spendingtracker.domain.repository.UnrecognizedMessageRepository
import com.aljaz.spendingtracker.domain.usecase.ConfirmTransactionsUseCase
import com.aljaz.spendingtracker.domain.usecase.ObserveCategoriesUseCase
import com.aljaz.spendingtracker.domain.usecase.ObserveUnreviewedTransactionsUseCase
import com.aljaz.spendingtracker.domain.usecase.UpdateTransactionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Logic behind the Review screen: confirms automatic transactions and
 * handles bank messages that couldn't be read.
 */
@HiltViewModel
class ReviewViewModel @Inject constructor(
    observeUnreviewed: ObserveUnreviewedTransactionsUseCase,
    observeCategories: ObserveCategoriesUseCase,
    private val confirmTransactions: ConfirmTransactionsUseCase,
    private val updateTransaction: UpdateTransactionUseCase,
    private val unrecognizedMessages: UnrecognizedMessageRepository,
    private val bankMessageProcessor: BankMessageProcessor
) : ViewModel() {

    /** Result of the last "Try again"; shown above the list. */
    private val notice = MutableStateFlow<String?>(null)

    /** Updates by itself whenever transactions, categories or messages change. */
    val uiState: StateFlow<ReviewUiState> = combine(
        observeUnreviewed(),
        unrecognizedMessages.observeAll(),
        observeCategories(),
        notice
    ) { transactions, unrecognized, categories, currentNotice ->
        ReviewUiState(
            transactions = transactions,
            unrecognized = unrecognized,
            categoriesById = categories.associateBy { it.id },
            notice = currentNotice,
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

    /**
     * Ticks or unticks "Split this between users" for one transaction.
     * Nothing is shared yet: shared expenses are uploaded only once confirmed.
     */
    fun onSplitToggle(transaction: Transaction, shared: Boolean) {
        val category = transaction.categoryId?.let { uiState.value.categoriesById[it] }
        val updated = if (shared) {
            transaction.copy(
                isShared = true,
                payerSharePercent = transaction.payerSharePercent
                    ?: category?.defaultSplitPercent
                    ?: DEFAULT_SPLIT_PERCENT
            )
        } else {
            transaction.copy(isShared = false, payerSharePercent = null)
        }
        viewModelScope.launch { updateTransaction(updated) }
    }

    /** Confirms every transaction currently in the queue. */
    fun onConfirmAll() {
        val ids = uiState.value.transactions.map { it.id }
        viewModelScope.launch { confirmTransactions(ids) }
    }

    /** Parses an unrecognized message again, e.g. after a parser update. */
    fun onRetry(message: UnrecognizedMessage) {
        viewModelScope.launch {
            notice.value = when (val result = bankMessageProcessor.retry(message)) {
                is BankMessageResult.Saved ->
                    "Recognized now and saved as \"${result.transaction.title}\"."
                BankMessageResult.Duplicate -> "Already saved earlier; removed from this list."
                BankMessageResult.Unrecognized -> "Still not recognized."
                BankMessageResult.NotFromBank -> "The sender is no longer a known bank."
            }
        }
    }

    /** Removes a message from the list for good. */
    fun onDelete(message: UnrecognizedMessage) {
        viewModelScope.launch { unrecognizedMessages.delete(message.id) }
    }

    private companion object {
        /** Used when neither the transaction nor its category has a share. */
        const val DEFAULT_SPLIT_PERCENT = 50
    }
}