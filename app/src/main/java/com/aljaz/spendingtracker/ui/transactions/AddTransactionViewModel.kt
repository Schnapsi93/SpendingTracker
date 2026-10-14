package com.aljaz.spendingtracker.ui.transactions

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aljaz.spendingtracker.core.AppResult
import com.aljaz.spendingtracker.core.MoneyParser
import com.aljaz.spendingtracker.domain.model.Transaction
import com.aljaz.spendingtracker.domain.model.TransactionType
import com.aljaz.spendingtracker.domain.usecase.AddTransactionUseCase
import com.aljaz.spendingtracker.domain.usecase.DeleteTransactionUseCase
import com.aljaz.spendingtracker.domain.usecase.GetTransactionUseCase
import com.aljaz.spendingtracker.domain.usecase.UpdateTransactionUseCase
import com.aljaz.spendingtracker.ui.navigation.TRANSACTION_ID_ARG
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Logic behind the add/edit transaction form.
 *
 * If the screen was opened with a transaction id (edit mode), the
 * transaction is loaded and the form is pre-filled. Otherwise the
 * form starts empty (add mode).
 */
@HiltViewModel
class AddTransactionViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getTransaction: GetTransactionUseCase,
    private val addTransaction: AddTransactionUseCase,
    private val updateTransaction: UpdateTransactionUseCase,
    private val deleteTransaction: DeleteTransactionUseCase
) : ViewModel() {

    /** Id from the navigation route, or null in add mode. */
    private val transactionId: Long? = savedStateHandle.get<Long>(TRANSACTION_ID_ARG)

    /**
     * The transaction as it was loaded, kept so that fields the form
     * doesn't show (source, category, raw text...) survive an edit.
     */
    private var original: Transaction? = null

    // Private and editable here; exposed to the screen as read-only.
    private val _uiState = MutableStateFlow(
        AddTransactionUiState(isEditing = transactionId != null)
    )
    val uiState: StateFlow<AddTransactionUiState> = _uiState.asStateFlow()

    init {
        transactionId?.let { loadTransaction(it) }
    }

    /** Loads an existing transaction and fills the form with it. */
    private fun loadTransaction(id: Long) {
        viewModelScope.launch {
            val transaction = getTransaction(id)
            if (transaction == null) {
                _uiState.update { it.copy(errorMessage = "Transaction not found.") }
                return@launch
            }

            original = transaction
            _uiState.update {
                it.copy(
                    type = transaction.type,
                    amountText = MoneyParser.formatForInput(transaction.amountMinor),
                    title = transaction.title,
                    note = transaction.note.orEmpty(),
                    timestamp = transaction.timestamp
                )
            }
        }
    }

    /** Switches between income and expense. */
    fun onTypeChange(type: TransactionType) =
        _uiState.update { it.copy(type = type) }

    /** Stores the amount text and clears any old error. */
    fun onAmountChange(text: String) =
        _uiState.update { it.copy(amountText = text, errorMessage = null) }

    /** Stores the title text. */
    fun onTitleChange(text: String) =
        _uiState.update { it.copy(title = text) }

    /** Stores the note text. */
    fun onNoteChange(text: String) =
        _uiState.update { it.copy(note = text) }

    /**
     * Validates the form, then adds a new transaction or updates the
     * existing one. On success, [AddTransactionUiState.isFinished] becomes true.
     */
    fun onSaveClick() {
        val state = _uiState.value
        if (state.isSaving) return // ignore double taps

        val amountMinor = MoneyParser.parseToMinor(state.amountText)
        if (amountMinor == null) {
            _uiState.update {
                it.copy(errorMessage = "Enter a valid amount, e.g. 12.50")
            }
            return
        }

        _uiState.update { it.copy(isSaving = true, errorMessage = null) }

        viewModelScope.launch {
            val existing = original

            val result: AppResult<Any> = if (existing == null) {
                addTransaction(
                    Transaction(
                        type = state.type,
                        amountMinor = amountMinor,
                        currency = DEFAULT_CURRENCY,
                        title = state.title,
                        note = state.note,
                        timestamp = state.timestamp
                    )
                )
            } else {
                updateTransaction(
                    existing.copy(
                        type = state.type,
                        amountMinor = amountMinor,
                        title = state.title,
                        note = state.note,
                        timestamp = state.timestamp,
                        // Saving an edit counts as the user confirming it.
                        reviewed = true
                    )
                )
            }

            when (result) {
                is AppResult.Success ->
                    _uiState.update { it.copy(isSaving = false, isFinished = true) }
                is AppResult.Error ->
                    _uiState.update {
                        it.copy(isSaving = false, errorMessage = result.message)
                    }
            }
        }
    }

    /** Opens the "Delete?" confirmation dialog. */
    fun onDeleteClick() =
        _uiState.update { it.copy(showDeleteConfirm = true) }

    /** Closes the confirmation dialog without deleting. */
    fun onDeleteDismiss() =
        _uiState.update { it.copy(showDeleteConfirm = false) }

    /** Deletes the transaction after the user confirmed. */
    fun onDeleteConfirm() {
        val existing = original ?: return
        _uiState.update { it.copy(showDeleteConfirm = false, isSaving = true) }

        viewModelScope.launch {
            deleteTransaction(existing)
            _uiState.update { it.copy(isSaving = false, isFinished = true) }
        }
    }

    private companion object {
        /** Temporary fixed currency. Becomes a user setting later. */
        const val DEFAULT_CURRENCY = "EUR"
    }
}