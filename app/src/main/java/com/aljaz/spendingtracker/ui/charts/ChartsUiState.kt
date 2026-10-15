package com.aljaz.spendingtracker.ui.charts

import com.aljaz.spendingtracker.domain.charts.CategoryBreakdown
import com.aljaz.spendingtracker.domain.model.Category
import java.time.YearMonth
import com.aljaz.spendingtracker.domain.charts.CashFlowPoint

/**
 * Everything the Charts screen shows at one moment.
 *
 * @property month the period shown
 * @property monthStartDay the day periods start on; 1 = calendar months
 * @property currentPeriod the period containing today, for the picker's "Today"
 * @property breakdown pie slices and list for the category chart
 * @property cashFlow income and expenses of the 12 periods ending with [month], oldest first
 * @property categoriesById all categories, for names and colors
 * @property isLoading true until the first data has arrived
 */
data class ChartsUiState(
    val month: YearMonth,
    val monthStartDay: Int = 1,
    val currentPeriod: YearMonth = month,
    val breakdown: CategoryBreakdown = CategoryBreakdown.EMPTY,
    val cashFlow: List<CashFlowPoint> = emptyList(),
    val categoriesById: Map<Long, Category> = emptyMap(),
    val isLoading: Boolean = true
)