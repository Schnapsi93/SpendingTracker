package com.aljaz.spendingtracker.ui.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aljaz.spendingtracker.core.AppResult
import com.aljaz.spendingtracker.core.MoneyParser
import com.aljaz.spendingtracker.domain.model.Transaction
import com.aljaz.spendingtracker.domain.model.TransactionType
import com.aljaz.spendingtracker.domain.usecase.AddTransactionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Logic behind the Add Transaction screen.
 *
 * Holds the form state, reacts to user input, and saves the
 * transaction through [AddTransactionUseCase].
 */
@HiltViewModel
class AddTransactionViewModel @Inject constructor(
    private val addTransaction: AddTransactionUseCase
) : ViewModel() {

    // Private and editable here; exposed to the screen as read-only.
    private val _uiState = MutableStateFlow(AddTransactionUiState())
    val uiState: StateFlow<AddTransactionUiState> = _uiState.asStateFlow()

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
     * Validates the form and saves the transaction.
     * On success, [AddTransactionUiState.saved] becomes true.
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
            val transaction = Transaction(
                type = state.type,
                amountMinor = amountMinor,
                currency = DEFAULT_CURRENCY,
                title = state.title,
                note = state.note,
                timestamp = state.timestamp
            )

            when (val result = addTransaction(transaction)) {
                is AppResult.Success ->
                    _uiState.update { it.copy(isSaving = false, saved = true) }
                is AppResult.Error ->
                    _uiState.update {
                        it.copy(isSaving = false, errorMessage = result.message)
                    }
            }
        }
    }

    private companion object {
        /** Temporary fixed currency. Becomes a user setting later. */
        const val DEFAULT_CURRENCY = "EUR"
    }
}