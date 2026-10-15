package com.aljaz.spendingtracker.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aljaz.spendingtracker.core.periodContaining
import com.aljaz.spendingtracker.domain.repository.UnrecognizedMessageRepository
import com.aljaz.spendingtracker.domain.settings.PeriodSettingsRepository
import com.aljaz.spendingtracker.domain.usecase.ObserveCategoriesUseCase
import com.aljaz.spendingtracker.domain.usecase.ObserveMonthOverviewUseCase
import com.aljaz.spendingtracker.domain.usecase.ObserveUnreviewedTransactionsUseCase
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
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

/**
 * Logic behind the Home screen: which period is selected, plus the
 * transactions, totals, categories and review counts needed to draw it.
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    observeMonthOverview: ObserveMonthOverviewUseCase,
    observeCategories: ObserveCategoriesUseCase,
    observeUnreviewed: ObserveUnreviewedTransactionsUseCase,
    unrecognizedMessages: UnrecognizedMessageRepository,
    private val periodSettings: PeriodSettingsRepository
) : ViewModel() {

    /** Starts at the period containing today (which may be last month's, e.g. on 10 Oct with start day 15). */
    private val selectedMonth = MutableStateFlow(
        periodContaining(LocalDate.now(), periodSettings.monthStartDay.value)
    )

    /**
     * Combines independent streams into one screen state:
     *
     * 1. The selected period's overview. It is recalculated whenever the
     *    selected month or the month start day changes; flatMapLatest drops
     *    the previous calculation.
     * 2. All categories, so each row can show its category's name and color.
     * 3. The number of automatic transactions waiting for review.
     * 4. The number of unreadable bank messages.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<HomeUiState> = combine(
        combine(selectedMonth, periodSettings.monthStartDay) { month, startDay -> month to startDay }
            .flatMapLatest { (month, startDay) ->
                observeMonthOverview(month, startDay).map { overview ->
                    Triple(month, startDay, overview)
                }
            },
        observeCategories(),
        observeUnreviewed().map { it.size },
        unrecognizedMessages.observeAll().map { it.size }
    ) { (month, startDay, overview), categories, unreviewedCount, unrecognizedCount ->
        HomeUiState(
            month = month,
            summary = overview.summary,
            expenseByCategory = overview.expenseByCategory,
            transactions = overview.transactions,
            categoriesById = categories.associateBy { it.id },
            unreviewedCount = unreviewedCount,
            unrecognizedCount = unrecognizedCount,
            monthStartDay = startDay,
            currentPeriod = periodContaining(LocalDate.now(), startDay)
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState(month = selectedMonth.value)
    )

    /** Moves the display one period back. */
    fun onPreviousMonth() = selectedMonth.update { it.minusMonths(1) }

    /** Moves the display one period forward. */
    fun onNextMonth() = selectedMonth.update { it.plusMonths(1) }

    /** Jumps directly to any period, e.g. one picked in the month selector. */
    fun onMonthSelected(month: YearMonth) {
        selectedMonth.value = month
    }
}