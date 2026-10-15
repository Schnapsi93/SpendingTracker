package com.aljaz.spendingtracker.ui.charts

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aljaz.spendingtracker.core.periodContaining
import com.aljaz.spendingtracker.domain.charts.CategoryBreakdownCalculator
import com.aljaz.spendingtracker.domain.settings.PeriodSettingsRepository
import com.aljaz.spendingtracker.domain.usecase.ObserveCategoriesUseCase
import com.aljaz.spendingtracker.domain.usecase.ObserveMonthOverviewUseCase
import com.aljaz.spendingtracker.ui.navigation.CHARTS_MONTH_ARG
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
 * Logic behind the Charts screen: which period is shown, and the
 * category breakdown for it.
 */
@HiltViewModel
class ChartsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    observeMonthOverview: ObserveMonthOverviewUseCase,
    observeCategories: ObserveCategoriesUseCase,
    periodSettings: PeriodSettingsRepository
) : ViewModel() {

    /** Starts at the period Home was showing, or the current period if none was passed. */
    private val selectedMonth = MutableStateFlow(
        savedStateHandle.get<String>(CHARTS_MONTH_ARG)
            ?.let { runCatching { YearMonth.parse(it) }.getOrNull() }
            ?: periodContaining(LocalDate.now(), periodSettings.monthStartDay.value)
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<ChartsUiState> = combine(
        combine(selectedMonth, periodSettings.monthStartDay) { month, startDay -> month to startDay }
            .flatMapLatest { (month, startDay) ->
                observeMonthOverview(month, startDay).map { overview ->
                    Triple(month, startDay, overview)
                }
            },
        observeCategories()
    ) { (month, startDay, overview), categories ->
        ChartsUiState(
            month = month,
            monthStartDay = startDay,
            currentPeriod = periodContaining(LocalDate.now(), startDay),
            breakdown = CategoryBreakdownCalculator.calculate(overview.expenseByCategory),
            categoriesById = categories.associateBy { it.id },
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ChartsUiState(month = selectedMonth.value)
    )

    fun onPreviousMonth() = selectedMonth.update { it.minusMonths(1) }

    fun onNextMonth() = selectedMonth.update { it.plusMonths(1) }

    fun onMonthSelected(month: YearMonth) {
        selectedMonth.value = month
    }
}