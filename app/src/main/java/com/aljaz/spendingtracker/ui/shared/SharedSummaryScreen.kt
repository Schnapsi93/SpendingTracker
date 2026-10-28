package com.aljaz.spendingtracker.ui.shared

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aljaz.spendingtracker.core.MoneyFormatter
import com.aljaz.spendingtracker.domain.sharing.SharedCategoryTotal
import com.aljaz.spendingtracker.domain.sharing.SharedTotals
import com.aljaz.spendingtracker.ui.components.PeriodHeader
import kotlin.math.abs

/** Currency used for the totals until multi-currency support exists. */
private const val DISPLAY_CURRENCY = "EUR"

/**
 * Shared expenses: who paid what in the selected period (in total and per
 * category), and who has to pay whom to be even.
 *
 * @param onBackClick called when the user wants to return to Home
 * @param viewModel provided by Hilt automatically
 */
@Composable
fun SharedSummaryScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SharedSummaryViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(modifier = modifier) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onBackClick) { Text("< Back") }
                Text(
                    text = "Shared expenses",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f)
                )
                if (state.isLoggedIn) {
                    TextButton(
                        onClick = viewModel::onSyncClick,
                        enabled = !state.syncStatus.isSyncing
                    ) {
                        Text(if (state.syncStatus.isSyncing) "Syncing…" else "Sync")
                    }
                }
            }

            if (!state.isLoggedIn) {
                Text(
                    text = "Log in under Settings → Shared expenses → Shared account " +
                            "to see the expenses you share.",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
                return@Column
            }

            PeriodHeader(
                month = state.month,
                monthStartDay = state.monthStartDay,
                currentPeriod = state.currentPeriod,
                onPreviousClick = viewModel::onPreviousMonth,
                onNextClick = viewModel::onNextMonth,
                onMonthSelected = viewModel::onMonthSelected
            )

            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                SettleCard(totals = state.overall, partnerName = state.partnerName)

                PeriodCard(totals = state.period.totals, partnerName = state.partnerName)

                if (state.period.byCategory.isNotEmpty()) {
                    CategoryCard(
                        categories = state.period.byCategory,
                        partnerName = state.partnerName
                    )
                }

                state.syncStatus.lastError?.let { error ->
                    Text(
                        text = "Last sync failed: $error",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

/**
 * The all-time result: what each person paid in total and who has to pay
 * whom to be even. Highlighted, because it is the answer most people look for.
 */
@Composable
private fun SettleCard(totals: SharedTotals, partnerName: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(text = "To settle up", style = MaterialTheme.typography.titleMedium)
            Text(
                text = settleText(totals.balanceMinor, partnerName),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold
            )
            AmountLine("You paid in total", totals.myPaidMinor)
            AmountLine("$partnerName paid in total", totals.partnerPaidMinor)
            Text(
                text = "Counts everything ever shared, not just this period.",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

/** The selected period: who paid, each person's share, and the period's own balance. */
@Composable
private fun PeriodCard(totals: SharedTotals, partnerName: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(text = "This period", style = MaterialTheme.typography.titleMedium)

            if (totals.totalMinor == 0L) {
                Text(
                    text = "No shared expenses in this period.",
                    style = MaterialTheme.typography.bodyMedium
                )
                return@Column
            }

            AmountLine("You paid", totals.myPaidMinor)
            AmountLine("$partnerName paid", totals.partnerPaidMinor)
            AmountLine("Total", totals.totalMinor, bold = true)
            HorizontalDivider()
            AmountLine("Your share", totals.myShareMinor)
            AmountLine("$partnerName's share", totals.partnerShareMinor)
            HorizontalDivider()
            Text(
                text = "This period alone: ${settleText(totals.balanceMinor, partnerName)}",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

/** Per-category table: what each person paid, and the total. */
@Composable
private fun CategoryCard(categories: List<SharedCategoryTotal>, partnerName: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(text = "By category", style = MaterialTheme.typography.titleMedium)

            // Header row
            CategoryTableRow(
                name = "Category",
                you = "You",
                partner = partnerName,
                total = "Total",
                isHeader = true
            )
            HorizontalDivider()

            categories.forEach { category ->
                CategoryTableRow(
                    name = category.category,
                    you = MoneyFormatter.format(category.myPaidMinor, DISPLAY_CURRENCY),
                    partner = MoneyFormatter.format(category.partnerPaidMinor, DISPLAY_CURRENCY),
                    total = MoneyFormatter.format(category.totalMinor, DISPLAY_CURRENCY)
                )
            }
        }
    }
}

/** One row of the category table; the name column is a little wider. */
@Composable
private fun CategoryTableRow(
    name: String,
    you: String,
    partner: String,
    total: String,
    isHeader: Boolean = false
) {
    val style = if (isHeader) {
        MaterialTheme.typography.labelMedium
    } else {
        MaterialTheme.typography.bodyMedium
    }
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = name,
            style = style,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1.3f)
        )
        listOf(you, partner, total).forEach { cell ->
            Text(
                text = cell,
                style = style,
                textAlign = TextAlign.End,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/** One label + amount row. */
@Composable
private fun AmountLine(label: String, amountMinor: Long, bold: Boolean = false) {
    val weight = if (bold) FontWeight.SemiBold else FontWeight.Normal
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontWeight = weight)
        Text(text = MoneyFormatter.format(amountMinor, DISPLAY_CURRENCY), fontWeight = weight)
    }
}

/**
 * Turns a balance into a sentence.
 * Positive: the partner owes this user. Negative: this user owes the partner.
 */
private fun settleText(balanceMinor: Long, partnerName: String): String {
    val amount = MoneyFormatter.format(abs(balanceMinor), DISPLAY_CURRENCY)
    return when {
        balanceMinor > 0 -> "$partnerName pays you $amount"
        balanceMinor < 0 -> "You pay $partnerName $amount"
        else -> "All settled up ✓"
    }
}