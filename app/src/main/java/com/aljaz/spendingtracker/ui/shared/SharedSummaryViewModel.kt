package com.aljaz.spendingtracker.ui.shared

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aljaz.spendingtracker.core.periodContaining
import com.aljaz.spendingtracker.core.toEpochMillisRange
import com.aljaz.spendingtracker.domain.settings.PeriodSettingsRepository
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

/**
 * Logic behind the shared summary screen: which period is shown, its
 * totals and categories, and the all-time balance that decides who pays whom.
 */
@HiltViewModel
class SharedSummaryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    expenses: SharedExpenseRepository,
    accounts: SharedAccountRepository,
    periodSettings: PeriodSettingsRepository,
    private val sync: SharedSyncRepository
) : ViewModel() {

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

    val uiState: StateFlow<SharedSummaryUiState> = combine(
        periodExpenses,
        expenses.observeAll(),
        accounts.account,
        sync.status
    ) { (month, startDay, periodList), allExpenses, account, syncStatus ->
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
                partnerName = account.partnerName ?: "Partner",
                period = SharedSummaryCalculator.calculate(periodList, account.userId),
                overall = SharedSummaryCalculator.calculate(allExpenses, account.userId).totals,
                syncStatus = syncStatus,
                isLoading = false
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SharedSummaryUiState(month = selectedMonth.value)
    )

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
}