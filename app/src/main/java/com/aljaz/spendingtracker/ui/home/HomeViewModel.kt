package com.aljaz.spendingtracker.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aljaz.spendingtracker.domain.usecase.ObserveCategoriesUseCase
import com.aljaz.spendingtracker.domain.usecase.ObserveMonthOverviewUseCase
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
import java.time.YearMonth
import javax.inject.Inject

/**
 * Logic behind the Home screen: which month is selected, plus the
 * transactions, totals and categories needed to draw it.
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    observeMonthOverview: ObserveMonthOverviewUseCase,
    observeCategories: ObserveCategoriesUseCase
) : ViewModel() {

    private val selectedMonth = MutableStateFlow(YearMonth.now())

    /**
     * Combines two independent streams into one screen state:
     *
     * 1. The selected month's overview. flatMapLatest switches to the new
     *    month whenever the selection changes and drops the old one.
     * 2. All categories, so each row can show its category's name and color.
     *
     * Whenever either stream emits (new transaction, renamed category,
     * different month), a fresh state is produced.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<HomeUiState> = combine(
        selectedMonth.flatMapLatest { month ->
            observeMonthOverview(month).map { overview -> month to overview }
        },
        observeCategories()
    ) { (month, overview), categories ->
        HomeUiState(
            month = month,
            summary = overview.summary,
            expenseByCategory = overview.expenseByCategory,
            transactions = overview.transactions,
            categoriesById = categories.associateBy { it.id }
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState(month = selectedMonth.value)
    )

    /** Moves the display one month back. */
    fun onPreviousMonth() = selectedMonth.update { it.minusMonths(1) }

    /** Moves the display one month forward. */
    fun onNextMonth() = selectedMonth.update { it.plusMonths(1) }

    /** Jumps directly to any month, e.g. one picked in the month selector. */
    fun onMonthSelected(month: YearMonth) {
        selectedMonth.value = month
    }
}