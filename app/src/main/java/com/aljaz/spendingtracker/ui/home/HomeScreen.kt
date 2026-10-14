package com.aljaz.spendingtracker.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aljaz.spendingtracker.core.MoneyFormatter
import com.aljaz.spendingtracker.domain.model.MonthSummary
import com.aljaz.spendingtracker.domain.model.Transaction
import com.aljaz.spendingtracker.domain.model.TransactionType
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** Currency used for the totals until multi-currency support exists. */
private const val DISPLAY_CURRENCY = "EUR"

/**
 * Home screen: month switcher, totals and the list of transactions.
 *
 * @param onAddTransactionClick called when the user taps the add button
 * @param viewModel provided by Hilt automatically
 */
@Composable
fun HomeScreen(
    onAddTransactionClick: () -> Unit,
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
            MonthHeader(
                month = state.month,
                onPreviousClick = viewModel::onPreviousMonth,
                onNextClick = viewModel::onNextMonth
            )
            SummaryCard(summary = state.summary)
            Spacer(modifier = Modifier.height(16.dp))

            if (state.transactions.isEmpty()) {
                Text(
                    text = "No transactions this month.",
                    style = MaterialTheme.typography.bodyLarge
                )
            } else {
                LazyColumn(
                    // Extra bottom space so the button never covers the last row.
                    contentPadding = PaddingValues(bottom = 88.dp)
                ) {
                    items(items = state.transactions, key = { it.id }) { transaction ->
                        TransactionRow(transaction)
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

/** Month title with previous / next buttons. */
@Composable
private fun MonthHeader(
    month: YearMonth,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        TextButton(onClick = onPreviousClick) { Text("<") }
        Text(
            text = month.format(DateTimeFormatter.ofPattern("LLLL yyyy")),
            style = MaterialTheme.typography.titleLarge
        )
        TextButton(onClick = onNextClick) { Text(">") }
    }
}

/** Card with income, expenses and balance for the month. */
@Composable
private fun SummaryCard(summary: MonthSummary) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SummaryLine("Income", summary.totalIncomeMinor)
            SummaryLine("Expenses", summary.totalExpenseMinor)
            HorizontalDivider()
            SummaryLine("Balance", summary.balanceMinor)
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

/** One transaction in the list: title and date on the left, amount on the right. */
@Composable
private fun TransactionRow(transaction: Transaction) {
    val isIncome = transaction.type == TransactionType.INCOME
    val sign = if (isIncome) "+" else "-"
    val amountColor =
        if (isIncome) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error

    val date = Instant.ofEpochMilli(transaction.timestamp)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
        .format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(text = transaction.title, style = MaterialTheme.typography.bodyLarge)
            Text(text = date, style = MaterialTheme.typography.bodySmall)
        }
        Text(
            text = sign + MoneyFormatter.format(transaction.amountMinor, transaction.currency),
            color = amountColor,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}