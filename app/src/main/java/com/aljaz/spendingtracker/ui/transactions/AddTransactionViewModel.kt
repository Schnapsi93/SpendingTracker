package com.aljaz.spendingtracker.ui.transactions

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aljaz.spendingtracker.core.AppResult
import com.aljaz.spendingtracker.core.MoneyParser
import com.aljaz.spendingtracker.domain.model.Transaction
import com.aljaz.spendingtracker.domain.model.TransactionType
import com.aljaz.spendingtracker.domain.usecase.AddTransactionUseCase
import com.aljaz.spendingtracker.domain.usecase.CreateRuleFromTransactionUseCase
import com.aljaz.spendingtracker.domain.usecase.DeleteTransactionUseCase
import com.aljaz.spendingtracker.domain.usecase.GetTransactionUseCase
import com.aljaz.spendingtracker.domain.usecase.ObserveCategoriesUseCase
import com.aljaz.spendingtracker.domain.usecase.UpdateTransactionUseCase
import com.aljaz.spendingtracker.ui.navigation.TRANSACTION_ID_ARG
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

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
    private val deleteTransaction: DeleteTransactionUseCase,
    private val observeCategories: ObserveCategoriesUseCase,
    private val createRuleFromTransaction: CreateRuleFromTransactionUseCase // NEW
) : ViewModel() {

    /** Id from the navigation route, or null in add mode. */
    private val transactionId: Long? = savedStateHandle.get<Long>(TRANSACTION_ID_ARG)

    /**
     * The transaction as it was loaded, kept so that fields the form
     * doesn't show (source, raw text...) survive an edit.
     */
    private var original: Transaction? = null

    // Private and editable here; exposed to the screen as read-only.
    private val _uiState = MutableStateFlow(
        AddTransactionUiState(isEditing = transactionId != null)
    )
    val uiState: StateFlow<AddTransactionUiState> = _uiState.asStateFlow()

    init {
        // Keep the category list up to date for the chips.
        viewModelScope.launch {
            observeCategories().collect { list ->
                _uiState.update { it.copy(categories = list) }
            }
        }
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
                    source = transaction.source, // NEW
                    type = transaction.type,
                    amountText = MoneyParser.formatForInput(transaction.amountMinor),
                    title = transaction.title,
                    note = transaction.note.orEmpty(),
                    timestamp = transaction.timestamp,
                    selectedCategoryId = transaction.categoryId
                )
            }
        }
    }

    /**
     * Switches between income and expense.
     * A category only fits one type, so the selection is dropped if it
     * no longer matches.
     */
    fun onTypeChange(type: TransactionType) = _uiState.update { state ->
        val selectionStillFits = state.categories.any {
            it.id == state.selectedCategoryId && it.type == type
        }
        state.copy(
            type = type,
            selectedCategoryId = if (selectionStillFits) state.selectedCategoryId else null
        )
    }

    /** Selects a category, or deselects it if it was already selected. */
    fun onCategoryClick(categoryId: Long) = _uiState.update {
        it.copy(
            selectedCategoryId = if (it.selectedCategoryId == categoryId) null else categoryId
        )
    }

    /** Stores the amount text and clears any old error. */
    fun onAmountChange(text: String) =
        _uiState.update { it.copy(amountText = text, errorMessage = null) }

    /** Stores the title text. */
    fun onTitleChange(text: String) =
        _uiState.update { it.copy(title = text) }

    /** Stores the note text. */
    fun onNoteChange(text: String) =
        _uiState.update { it.copy(note = text) }


    /** Changes the date and keeps the time of day. */
    fun onDateSelected(date: LocalDate) = _uiState.update { state ->
        val zone = ZoneId.systemDefault()
        val time = Instant.ofEpochMilli(state.timestamp).atZone(zone).toLocalTime()
        state.copy(timestamp = date.atTime(time).atZone(zone).toInstant().toEpochMilli())
    }

    /** Changes the time of day and keeps the date. */
    fun onTimeSelected(time: LocalTime) = _uiState.update { state ->
        val zone = ZoneId.systemDefault()
        val date = Instant.ofEpochMilli(state.timestamp).atZone(zone).toLocalDate()
        state.copy(timestamp = date.atTime(time).atZone(zone).toInstant().toEpochMilli())
    }

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
                        categoryId = state.selectedCategoryId,
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
                        categoryId = state.selectedCategoryId,
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



    /** Opens the rule dialog, pre-filled with the merchant name. */
    fun onApplyToFutureClick() = _uiState.update {
        it.copy(ruleDialog = RuleDialogState(keyword = original?.title ?: it.title))
    }

    /** Stores the edited keyword and clears any old error. */
    fun onRuleKeywordChange(value: String) = _uiState.update { state ->
        state.copy(ruleDialog = state.ruleDialog?.copy(keyword = value, error = null))
    }

    /** Closes the rule dialog without creating a rule. */
    fun onRuleDialogDismiss() =
        _uiState.update { it.copy(ruleDialog = null) }

    /**
     * Creates the rule, then saves the transaction (which also closes the form).
     * The rule is checked against the merchant as the bank sent it, even if
     * the user changed the title.
     */
    fun onRuleConfirm() {
        val state = _uiState.value
        val dialog = state.ruleDialog ?: return
        val categoryId = state.selectedCategoryId ?: return

        // Check the amount first, so a rule is never created for a form that can't be saved.
        if (MoneyParser.parseToMinor(state.amountText) == null) {
            _uiState.update {
                it.copy(ruleDialog = null, errorMessage = "Enter a valid amount, e.g. 12.50")
            }
            return
        }

        val merchant = original?.title ?: state.title

        viewModelScope.launch {
            when (val result = createRuleFromTransaction(dialog.keyword, merchant, categoryId)) {
                is AppResult.Error -> _uiState.update {
                    it.copy(ruleDialog = it.ruleDialog?.copy(error = result.message))
                }
                is AppResult.Success -> {
                    _uiState.update { it.copy(ruleDialog = null) }
                    onSaveClick()
                }
            }
        }
    }

    // ---------- Delete ----------

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