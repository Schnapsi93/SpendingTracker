package com.aljaz.spendingtracker.ui.shared

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aljaz.spendingtracker.core.AppResult
import com.aljaz.spendingtracker.core.MoneyParser
import com.aljaz.spendingtracker.core.periodContaining
import com.aljaz.spendingtracker.core.toEpochMillisRange
import com.aljaz.spendingtracker.domain.settings.PeriodSettingsRepository
import com.aljaz.spendingtracker.domain.sharing.Settlement
import com.aljaz.spendingtracker.domain.sharing.SettlementRepository
import com.aljaz.spendingtracker.domain.sharing.SharedAccountRepository
import com.aljaz.spendingtracker.domain.sharing.SharedExpenseRepository
import com.aljaz.spendingtracker.domain.sharing.SharedSummaryCalculator
import com.aljaz.spendingtracker.domain.sharing.SharedSyncRepository
import com.aljaz.spendingtracker.ui.navigation.SHARED_MONTH_ARG
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject
import kotlin.math.abs

/**
 * Logic behind the shared summary screen: which period is shown, its
 * totals and categories, the all-time balance, and settle-up payments.
 */
@HiltViewModel
class SharedSummaryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    expenses: SharedExpenseRepository,
    accounts: SharedAccountRepository,
    periodSettings: PeriodSettingsRepository,
    private val settlementRepository: SettlementRepository,
    private val sync: SharedSyncRepository
) : ViewModel() {

    /** Dialogs and messages; kept apart from the data so both can update independently. */
    private data class EditorState(
        val dialog: SettleDialogState? = null,
        val pendingDelete: Settlement? = null,
        val message: String? = null
    )

    private val editor = MutableStateFlow(EditorState())

    /** Starts at the period Home was showing, or the current period if none was passed. */
    private val selectedMonth = MutableStateFlow(
        savedStateHandle.get<String>(SHARED_MONTH_ARG)
            ?.let { runCatching { YearMonth.parse(it) }.getOrNull() }
            ?: periodContaining(LocalDate.now(), periodSettings.monthStartDay.value)
    )

    /** The selected period's expenses, reloaded whenever the period or start day changes. */
    @OptIn(ExperimentalCoroutinesApi::class)
    private val periodExpenses = combine(selectedMonth, periodSettings.monthStartDay) { month, startDay ->
        month to startDay
    }.flatMapLatest { (month, startDay) ->
        val (start, end) = month.toEpochMillisRange(startDay = startDay)
        expenses.observeBetween(start, end).map { list -> Triple(month, startDay, list) }
    }

    /** Everything that comes from the database and the account. */
    private val data = combine(
        periodExpenses,
        expenses.observeAll(),
        settlementRepository.observeAll(),
        accounts.account,
        sync.status
    ) { (month, startDay, periodList), allExpenses, allSettlements, account, syncStatus ->
        val currentPeriod = periodContaining(LocalDate.now(), startDay)

        if (account == null) {
            SharedSummaryUiState(
                month = month,
                monthStartDay = startDay,
                currentPeriod = currentPeriod,
                isLoggedIn = false,
                isLoading = false
            )
        } else {
            SharedSummaryUiState(
                month = month,
                monthStartDay = startDay,
                currentPeriod = currentPeriod,
                isLoggedIn = true,
                myUserId = account.userId,
                partnerId = account.partnerId,
                partnerName = account.partnerName ?: "Partner",
                period = SharedSummaryCalculator.calculate(periodList, account.userId),
                overall = SharedSummaryCalculator
                    .calculate(allExpenses, account.userId, allSettlements)
                    .totals,
                settlements = allSettlements,
                syncStatus = syncStatus,
                isLoading = false
            )
        }
    }

    val uiState: StateFlow<SharedSummaryUiState> = combine(data, editor) { state, edit ->
        state.copy(
            settleDialog = edit.dialog,
            pendingDeleteSettlement = edit.pendingDelete,
            message = edit.message
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SharedSummaryUiState(month = selectedMonth.value)
    )

    // ---------- Period and sync ----------

    fun onPreviousMonth() = selectedMonth.update { it.minusMonths(1) }

    fun onNextMonth() = selectedMonth.update { it.plusMonths(1) }

    fun onMonthSelected(month: YearMonth) {
        selectedMonth.value = month
    }

    /** Downloads the partner's newest expenses right away. */
    fun onSyncClick() {
        if (uiState.value.syncStatus.isSyncing) return
        viewModelScope.launch { sync.syncNow() }
    }

    // ---------- Record a payment ----------

    /** Opens the dialog, pre-filled with the open balance and its direction. */
    fun onSettleClick() {
        val balance = uiState.value.overall.balanceMinor
        editor.update {
            it.copy(
                dialog = SettleDialogState(
                    // Positive balance: the partner owes me, so the partner pays.
                    partnerPaysMe = balance >= 0,
                    amountText = if (balance != 0L) MoneyParser.formatForInput(abs(balance)) else ""
                ),
                message = null
            )
        }
    }

    fun onSettleDirectionChange(partnerPaysMe: Boolean) =
        updateDialog { it.copy(partnerPaysMe = partnerPaysMe, errorMessage = null) }

    fun onSettleAmountChange(text: String) =
        updateDialog { it.copy(amountText = text, errorMessage = null) }

    fun onSettleNoteChange(text: String) =
        updateDialog { it.copy(note = text) }

    fun onSettleDismiss() =
        editor.update { it.copy(dialog = null) }

    /** Sends the payment to the server; closes the dialog on success. */
    fun onSettleConfirm() {
        val dialog = editor.value.dialog ?: return
        if (dialog.isSaving) return

        val state = uiState.value
        val partnerId = state.partnerId ?: return

        val amount = MoneyParser.parseToMinor(dialog.amountText)
        if (amount == null || amount <= 0) {
            updateDialog { it.copy(errorMessage = "Enter a valid amount, e.g. 43.20") }
            return
        }

        val (from, to) = if (dialog.partnerPaysMe) {
            partnerId to state.myUserId
        } else {
            state.myUserId to partnerId
        }

        updateDialog { it.copy(isSaving = true, errorMessage = null) }
        viewModelScope.launch {
            when (val result = settlementRepository.record(from, to, amount, dialog.note)) {
                is AppResult.Success -> editor.update { it.copy(dialog = null) }
                is AppResult.Error ->
                    updateDialog { it.copy(isSaving = false, errorMessage = result.message) }
            }
        }
    }

    // ---------- Undo a payment ----------

    /** Asks for confirmation first. */
    fun onDeleteSettlementClick(settlement: Settlement) =
        editor.update { it.copy(pendingDelete = settlement, message = null) }

    fun onDeleteSettlementDismiss() =
        editor.update { it.copy(pendingDelete = null) }

    fun onDeleteSettlementConfirm() {
        val settlement = editor.value.pendingDelete ?: return
        editor.update { it.copy(pendingDelete = null) }
        viewModelScope.launch {
            val result = settlementRepository.delete(settlement)
            if (result is AppResult.Error) {
                editor.update { it.copy(message = result.message) }
            }
        }
    }

    /** Applies a change to the open dialog; does nothing if it is closed. */
    private fun updateDialog(change: (SettleDialogState) -> SettleDialogState) =
        editor.update { state ->
            state.dialog?.let { state.copy(dialog = change(it)) } ?: state
        }
}