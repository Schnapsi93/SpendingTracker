package com.aljaz.spendingtracker.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aljaz.spendingtracker.domain.usecase.ObserveMonthOverviewUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.YearMonth
import javax.inject.Inject

/**
 * Logic behind the Home screen: which month is selected, and the
 * transactions and totals for that month.
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    observeMonthOverview: ObserveMonthOverviewUseCase
) : ViewModel() {

    private val selectedMonth = MutableStateFlow(YearMonth.now())

    /**
     * Whenever the selected month changes, switch to observing the new
     * month (flatMapLatest cancels the old one). Whenever the data in
     * the database changes, a new state is emitted automatically.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<HomeUiState> = selectedMonth
        .flatMapLatest { month ->
            observeMonthOverview(month).map { overview ->
                HomeUiState(
                    month = month,
                    summary = overview.summary,
                    transactions = overview.transactions
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HomeUiState(month = selectedMonth.value)
        )

    /** Moves the display one month back. */
    fun onPreviousMonth() = selectedMonth.update { it.minusMonths(1) }

    /** Moves the display one month forward. */
    fun onNextMonth() = selectedMonth.update { it.plusMonths(1) }
}