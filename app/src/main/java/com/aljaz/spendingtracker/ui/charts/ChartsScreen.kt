package com.aljaz.spendingtracker.ui.charts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aljaz.spendingtracker.core.MoneyFormatter
import com.aljaz.spendingtracker.domain.charts.ChartSlice
import com.aljaz.spendingtracker.ui.components.ColorDot
import com.aljaz.spendingtracker.ui.components.PeriodHeader
import kotlin.math.roundToInt

/** Currency used for the totals until multi-currency support exists. */
private const val DISPLAY_CURRENCY = "EUR"

/** The two chart views. */
private enum class ChartTab { CATEGORIES, CASH_FLOW }

/**
 * Charts for one period: spending by category, and (next step) cash flow.
 *
 * @param onBackClick called when the user wants to return to Home
 * @param viewModel provided by Hilt automatically
 */
@Composable
fun ChartsScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ChartsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var tab by rememberSaveable { mutableStateOf(ChartTab.CATEGORIES) }

    Scaffold(modifier = modifier) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onBackClick) { Text("< Back") }
                Text(text = "Charts", style = MaterialTheme.typography.titleLarge)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = tab == ChartTab.CATEGORIES,
                    onClick = { tab = ChartTab.CATEGORIES },
                    label = { Text("By category") }
                )
                FilterChip(
                    selected = tab == ChartTab.CASH_FLOW,
                    onClick = { tab = ChartTab.CASH_FLOW },
                    label = { Text("Cash flow") }
                )
            }

            PeriodHeader(
                month = state.month,
                monthStartDay = state.monthStartDay,
                currentPeriod = state.currentPeriod,
                onPreviousClick = viewModel::onPreviousMonth,
                onNextClick = viewModel::onNextMonth,
                onMonthSelected = viewModel::onMonthSelected
            )

            when (tab) {
                ChartTab.CATEGORIES -> CategoryChart(state)
                ChartTab.CASH_FLOW -> Text(
                    text = "The cash flow chart comes in the next step.",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}

/** Pie chart of the period's expenses with the list of categories next to it. */
@Composable
private fun CategoryChart(state: ChartsUiState) {
    val breakdown = state.breakdown

    if (state.isLoading) return
    if (breakdown.totalMinor == 0L) {
        Text(text = "No expenses in this period.", style = MaterialTheme.typography.bodyLarge)
        return
    }

    // Colors and names for "Other" and "Uncategorized", which have no category of their own.
    val otherColor = MaterialTheme.colorScheme.outline
    val uncategorizedColor = MaterialTheme.colorScheme.outlineVariant

    fun colorOf(slice: ChartSlice): Color = when {
        slice.isOther -> otherColor
        slice.categoryId == null -> uncategorizedColor
        else -> state.categoriesById[slice.categoryId]?.let { Color(it.colorArgb) } ?: uncategorizedColor
    }

    fun nameOf(slice: ChartSlice): String = when {
        slice.isOther -> "Other"
        slice.categoryId == null -> "Uncategorized"
        else -> state.categoriesById[slice.categoryId]?.name ?: "Unknown"
    }

    Text(
        text = "Expenses: ${MoneyFormatter.format(breakdown.totalMinor, DISPLAY_CURRENCY)}",
        style = MaterialTheme.typography.titleMedium
    )

    Row(verticalAlignment = Alignment.CenterVertically) {
        PieChart(
            slices = breakdown.slices.map { slice ->
                PieSlice(
                    fraction = slice.share.toFloat(),
                    color = colorOf(slice),
                    // Very thin slices get no label; the list shows their percentage.
                    label = percentText(slice.share).takeIf { slice.share >= 0.05 }
                )
            },
            modifier = Modifier.weight(0.45f)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(
            modifier = Modifier.weight(0.55f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            breakdown.legend.forEach { slice ->
                LegendItem(
                    color = colorOf(slice),
                    name = nameOf(slice),
                    amount = MoneyFormatter.format(slice.amountMinor, DISPLAY_CURRENCY),
                    percent = percentText(slice.share)
                )
            }
        }
    }
}

/** One list entry: dot and name, then amount and percentage below. */
@Composable
private fun LegendItem(color: Color, name: String, amount: String, percent: String) {
    Row(verticalAlignment = Alignment.Top) {
        ColorDot(colorArgb = color.toArgb(), modifier = Modifier.padding(top = 4.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                text = name,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(text = "$amount · $percent", style = MaterialTheme.typography.bodySmall)
        }
    }
}

/** 0.253 becomes "25 %"; tiny but non-zero shares show as "<1 %". */
private fun percentText(share: Double): String {
    val percent = (share * 100).roundToInt()
    return if (percent == 0 && share > 0) "<1 %" else "$percent %"
}