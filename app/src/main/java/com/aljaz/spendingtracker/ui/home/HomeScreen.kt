package com.aljaz.spendingtracker.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aljaz.spendingtracker.core.MoneyFormatter
import com.aljaz.spendingtracker.domain.model.Category
import com.aljaz.spendingtracker.domain.model.CategoryTotal
import com.aljaz.spendingtracker.domain.model.MonthSummary
import com.aljaz.spendingtracker.ui.components.ColorDot
import com.aljaz.spendingtracker.ui.components.PeriodHeader
import com.aljaz.spendingtracker.ui.components.TransactionRow
import java.time.YearMonth


/** Currency used for the totals until multi-currency support exists. */
private const val DISPLAY_CURRENCY = "EUR"

/**
 * Home screen: month header, then one scrolling list containing the
 * month totals, spending per category and the transactions.
 *
 * @param onAddTransactionClick called when the user taps the add button
 * @param onTransactionClick called with a transaction's id when its row is tapped
 * @param onCategoriesClick called when the user wants to manage categories
 * @param onSettingsClick called when the user wants to open the settings
 * @param onReviewClick called when the user opens the review queue
 * @param onSearchClick called when the user opens the search
 * @param onChartsClick called with the shown period when the user opens the charts
 * @param viewModel provided by Hilt automatically
 */
@Composable
fun HomeScreen(
    onAddTransactionClick: () -> Unit,
    onTransactionClick: (Long) -> Unit,
    onCategoriesClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onReviewClick: () -> Unit,
    onSearchClick: () -> Unit,
    onChartsClick: (YearMonth) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        modifier = modifier,
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = onAddTransactionClick) {
                Text("Add transaction")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            // App title with a shortcut to category management.
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Takes the free space on the left; Categories and Settings stay on the right.
                OutlinedButton(
                    onClick = onSearchClick,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Search…")
                }
                TextButton(onClick = onCategoriesClick) { Text("Categories") }
                TextButton(onClick = onSettingsClick) { Text("Settings") }
            }

            // The header stays fixed at the top; everything below scrolls.
            PeriodHeader(
                month = state.month,
                monthStartDay = state.monthStartDay,
                currentPeriod = state.currentPeriod,
                onPreviousClick = viewModel::onPreviousMonth,
                onNextClick = viewModel::onNextMonth,
                onMonthSelected = viewModel::onMonthSelected
            )

            LazyColumn(
                // Extra bottom space so the button never covers the last row.
                contentPadding = PaddingValues(bottom = 88.dp)
            ) {
                // Shown while automatic transactions or unreadable messages need attention.
                if (state.unreviewedCount > 0 || state.unrecognizedCount > 0) {
                    item(key = "review_banner") {
                        ReviewBanner(
                            unreviewedCount = state.unreviewedCount,
                            unrecognizedCount = state.unrecognizedCount,
                            onClick = onReviewClick
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
                item(key = "summary") {
                    SummaryCard(
                        summary = state.summary,
                        carryOver = state.carryOver,
                        onChartsClick = { onChartsClick(state.month) }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Hidden in months without expenses.
                if (state.expenseByCategory.isNotEmpty()) {
                    item(key = "categories") {
                        CategoryBreakdownCard(
                            totals = state.expenseByCategory,
                            categoriesById = state.categoriesById
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }

                item(key = "transactions_title") {
                    Text(
                        text = "Transactions",
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                if (state.transactions.isEmpty()) {
                    item(key = "empty") {
                        Text(
                            text = "No transactions this month.",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    }
                } else {
                    items(items = state.transactions, key = { it.id }) { transaction ->
                        TransactionRow(
                            transaction = transaction,
                            category = transaction.categoryId?.let { state.categoriesById[it] },
                            onClick = { onTransactionClick(transaction.id) }
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}



/**
 * Card with the period's totals. With carry-over on, the balance left
 * from earlier periods is shown first and included in the balance.
 */
@Composable
private fun SummaryCard(
    summary: MonthSummary,
    carryOver: Boolean,
    onChartsClick: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (carryOver) {
                SummaryLine("Carried over", summary.carriedOverMinor)
            }
            SummaryLine("Income", summary.totalIncomeMinor)
            SummaryLine("Expenses", summary.totalExpenseMinor)
            HorizontalDivider()
            SummaryLine("Balance", summary.balanceMinor)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onChartsClick) { Text("Charts >") }
            }
        }
    }
}

/** One label + amount row inside the summary card. */
@Composable
private fun SummaryLine(label: String, amountMinor: Long) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label)
        Text(text = MoneyFormatter.format(amountMinor, DISPLAY_CURRENCY))
    }
}

/**
 * Card listing how much was spent per category, largest first.
 *
 * @param totals expense totals per category, already sorted
 * @param categoriesById used to look up names and colors
 */
@Composable
private fun CategoryBreakdownCard(
    totals: List<CategoryTotal>,
    categoriesById: Map<Long, Category>
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(text = "Spending by category", style = MaterialTheme.typography.titleMedium)

            totals.forEach { total ->
                CategoryTotalRow(
                    category = total.categoryId?.let { categoriesById[it] },
                    amountMinor = total.totalMinor
                )
            }
        }
    }
}

/**
 * One category inside [CategoryBreakdownCard]: dot, name and sum.
 *
 * @param category the category, or null for uncategorized spending
 */
@Composable
private fun CategoryTotalRow(
    category: Category?,
    amountMinor: Long
) {
    // Uncategorized spending uses the same neutral grey as in the list.
    val dotColor = category?.colorArgb
        ?: MaterialTheme.colorScheme.outlineVariant.toArgb()

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ColorDot(colorArgb = dotColor)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = category?.name ?: "Uncategorized",
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(text = MoneyFormatter.format(amountMinor, DISPLAY_CURRENCY))
    }
}

/**
 * Highlighted card telling the user how many automatic transactions
 * await review and how many bank messages couldn't be read.
 */
@Composable
private fun ReviewBanner(
    unreviewedCount: Int,
    unrecognizedCount: Int,
    onClick: () -> Unit
) {
    // Builds e.g. "2 automatic transactions to review · 1 message couldn't be read"
    val parts = buildList {
        if (unreviewedCount == 1) add("1 automatic transaction to review")
        if (unreviewedCount > 1) add("$unreviewedCount automatic transactions to review")
        if (unrecognizedCount == 1) add("1 message couldn't be read")
        if (unrecognizedCount > 1) add("$unrecognizedCount messages couldn't be read")
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = parts.joinToString(" · "),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
            Text(text = "Review >", style = MaterialTheme.typography.labelLarge)
        }
    }
}