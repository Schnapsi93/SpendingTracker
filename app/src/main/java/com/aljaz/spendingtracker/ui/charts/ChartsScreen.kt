package com.aljaz.spendingtracker.ui.charts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
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
import com.aljaz.spendingtracker.domain.charts.CategoryBreakdownCalculator
import com.aljaz.spendingtracker.ui.components.ColorDot
import com.aljaz.spendingtracker.ui.components.PeriodHeader
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

/** Currency used for the totals until multi-currency support exists. */
private const val DISPLAY_CURRENCY = "EUR"

/**
 * Charts for one period on a single screen:
 * the pie of expenses by category with its list on top, the cash flow below.
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

    Scaffold(modifier = modifier) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                // Scrolls only if the exact amounts are opened or the screen is very small.
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onBackClick) { Text("< Back") }
                Text(text = "Charts", style = MaterialTheme.typography.titleLarge)
            }

            PeriodHeader(
                month = state.month,
                monthStartDay = state.monthStartDay,
                currentPeriod = state.currentPeriod,
                onPreviousClick = viewModel::onPreviousMonth,
                onNextClick = viewModel::onNextMonth,
                onMonthSelected = viewModel::onMonthSelected
            )

            if (!state.isLoading) {
                CategorySection(state)
                HorizontalDivider()
                CashFlowSection(state)
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

/**
 * Pie chart of the period's expenses with the list of categories to its right.
 * The list shows exactly the pie's slices, including the merged "Other".
 */
@Composable
private fun CategorySection(state: ChartsUiState) {
    val breakdown = state.breakdown

    Text(
        text = "Expenses: ${MoneyFormatter.format(breakdown.totalMinor, DISPLAY_CURRENCY)}",
        style = MaterialTheme.typography.titleMedium
    )

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

    Row(verticalAlignment = Alignment.CenterVertically) {
        PieChart(
            slices = breakdown.slices.map { slice ->
                PieSlice(
                    fraction = slice.share.toFloat(),
                    color = colorOf(slice),
                    // Thin slices get no label; the list next to the pie shows their percentage.
                    label = percentText(slice.share)
                        .takeIf { slice.share >= CategoryBreakdownCalculator.MIN_SLICE_SHARE }
                )
            },
            modifier = Modifier.weight(0.45f)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(
            modifier = Modifier.weight(0.55f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            breakdown.slices.forEach { slice ->
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

/** One list entry: dot and name, then amount and percentage below in smaller text. */
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
            Text(
                text = "$amount · $percent",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** 0.253 becomes "25 %"; tiny but non-zero shares show as "<1 %". */
private fun percentText(share: Double): String {
    val percent = (share * 100).roundToInt()
    return if (percent == 0 && share > 0) "<1 %" else "$percent %"
}

/**
 * Cash flow of the 12 periods ending with the selected one: the line chart and
 * its legend. The table with exact amounts is folded away until it is asked for,
 * so both charts fit on one screen.
 */
@Composable
private fun CashFlowSection(state: ChartsUiState) {
    var showAmounts by rememberSaveable { mutableStateOf(false) }

    Text(text = "Cash flow · last 12 months", style = MaterialTheme.typography.titleMedium)

    val points = state.cashFlow
    if (points.all { it.incomeMinor == 0L && it.expenseMinor == 0L }) {
        Text(
            text = "No transactions in these 12 months.",
            style = MaterialTheme.typography.bodyLarge
        )
        return
    }

    CashFlowChart(
        points = points,
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
    )

    // Legend on the left, the button for the exact amounts on the right.
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LineLegend(color = CashFlowColors.Income, text = "Income")
        Spacer(modifier = Modifier.width(16.dp))
        LineLegend(color = CashFlowColors.Expense, text = "Expenses")
        Spacer(modifier = Modifier.weight(1f))
        TextButton(onClick = { showAmounts = !showAmounts }) {
            Text(if (showAmounts) "Hide amounts" else "Show amounts")
        }
    }

    if (showAmounts) {
        CashFlowTable(state)
    }
}

/** A colored dot with the name of a chart line. */
@Composable
private fun LineLegend(color: Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        ColorDot(colorArgb = color.toArgb())
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = text, style = MaterialTheme.typography.bodyMedium)
    }
}

/** Exact income and expenses per period, newest first. */
@Composable
private fun CashFlowTable(state: ChartsUiState) {
    val monthFormat = DateTimeFormatter.ofPattern("LLL yyyy")
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        state.cashFlow.asReversed().forEach { point ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = point.month.format(monthFormat),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "+" + MoneyFormatter.format(point.incomeMinor, DISPLAY_CURRENCY),
                    color = CashFlowColors.Income,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "−" + MoneyFormatter.format(point.expenseMinor, DISPLAY_CURRENCY),
                    color = CashFlowColors.Expense,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}