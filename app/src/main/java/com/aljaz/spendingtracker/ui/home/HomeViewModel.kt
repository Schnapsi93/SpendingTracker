package com.aljaz.spendingtracker.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aljaz.spendingtracker.core.periodContaining
import com.aljaz.spendingtracker.domain.model.MonthOverview
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
     *    selected month, the month start day or the carry-over setting
     *    changes; flatMapLatest drops the previous calculation.
     * 2. All categories, so each row can show its category's name and color.
     * 3. The number of automatic transactions waiting for review.
     * 4. The number of unreadable bank messages.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<HomeUiState> = combine(
        combine(
            selectedMonth,
            periodSettings.monthStartDay,
            periodSettings.carryOver
        ) { month, startDay, carryOver -> Triple(month, startDay, carryOver) }
            .flatMapLatest { (month, startDay, carryOver) ->
                observeMonthOverview(month, startDay, carryOver).map { overview ->
                    PeriodView(month, startDay, carryOver, overview)
                }
            },
        observeCategories(),
        observeUnreviewed().map { it.size },
        unrecognizedMessages.observeAll().map { it.size }
    ) { view, categories, unreviewedCount, unrecognizedCount ->
        HomeUiState(
            month = view.month,
            summary = view.overview.summary,
            expenseByCategory = view.overview.expenseByCategory,
            transactions = view.overview.transactions,
            categoriesById = categories.associateBy { it.id },
            unreviewedCount = unreviewedCount,
            unrecognizedCount = unrecognizedCount,
            monthStartDay = view.startDay,
            currentPeriod = periodContaining(LocalDate.now(), view.startDay),
            carryOver = view.carryOver
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

/** One calculated period together with the settings it was calculated with. */
private data class PeriodView(
    val month: YearMonth,
    val startDay: Int,
    val carryOver: Boolean,
    val overview: MonthOverview
)