package com.aljaz.spendingtracker.ui.shared

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aljaz.spendingtracker.core.MoneyFormatter
import com.aljaz.spendingtracker.domain.sharing.Settlement
import com.aljaz.spendingtracker.domain.sharing.SharedCategoryTotal
import com.aljaz.spendingtracker.domain.sharing.SharedTotals
import com.aljaz.spendingtracker.ui.components.PeriodHeader
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlin.math.abs

/** Currency used for the totals until multi-currency support exists. */
private const val DISPLAY_CURRENCY = "EUR"

/**
 * Shared expenses: who paid what in the selected period (in total and per
 * category), who has to pay whom to be even, and the settle-up payments.
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
                SettleCard(
                    totals = state.overall,
                    partnerName = state.partnerName,
                    canSettle = state.canSettle,
                    onSettleClick = viewModel::onSettleClick
                )

                state.message?.let { message ->
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                PeriodCard(totals = state.period.totals, partnerName = state.partnerName)

                if (state.period.byCategory.isNotEmpty()) {
                    CategoryCard(
                        categories = state.period.byCategory,
                        partnerName = state.partnerName
                    )
                }

                if (state.settlements.isNotEmpty()) {
                    SettlementsCard(
                        settlements = state.settlements,
                        myUserId = state.myUserId,
                        partnerName = state.partnerName,
                        onUndoClick = viewModel::onDeleteSettlementClick
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

    // "Record a payment" dialog.
    state.settleDialog?.let { dialog ->
        SettleDialog(
            dialog = dialog,
            partnerName = state.partnerName,
            onDirectionChange = viewModel::onSettleDirectionChange,
            onAmountChange = viewModel::onSettleAmountChange,
            onNoteChange = viewModel::onSettleNoteChange,
            onConfirm = viewModel::onSettleConfirm,
            onDismiss = viewModel::onSettleDismiss
        )
    }

    // "Undo payment?" confirmation.
    state.pendingDeleteSettlement?.let { settlement ->
        AlertDialog(
            onDismissRequest = viewModel::onDeleteSettlementDismiss,
            title = { Text("Undo this payment?") },
            text = {
                Text(
                    "${settlementText(settlement, state.myUserId, state.partnerName)} " +
                            "${MoneyFormatter.format(settlement.amountMinor, DISPLAY_CURRENCY)}. " +
                            "It is removed on both phones and the balance goes back up."
                )
            },
            confirmButton = {
                TextButton(onClick = viewModel::onDeleteSettlementConfirm) { Text("Undo payment") }
            },
            dismissButton = {
                TextButton(onClick = viewModel::onDeleteSettlementDismiss) { Text("Cancel") }
            }
        )
    }
}

/**
 * The all-time result: what each person paid in total and who has to pay
 * whom to be even. Highlighted, because it is the answer most people look for.
 */
@Composable
private fun SettleCard(
    totals: SharedTotals,
    partnerName: String,
    canSettle: Boolean,
    onSettleClick: () -> Unit
) {
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
            if (totals.settledMinor != 0L) {
                val settled = MoneyFormatter.format(abs(totals.settledMinor), DISPLAY_CURRENCY)
                Text(
                    text = if (totals.settledMinor > 0) {
                        "Settle-up payments so far: you paid $partnerName $settled"
                    } else {
                        "Settle-up payments so far: $partnerName paid you $settled"
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Text(
                text = "Counts everything ever shared, not just this period.",
                style = MaterialTheme.typography.bodySmall
            )

            if (canSettle) {
                Button(onClick = onSettleClick) {
                    Text(if (totals.balanceMinor != 0L) "Settle up…" else "Record a payment…")
                }
            }
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

/** History of settle-up payments, newest first, each with an "Undo" button. */
@Composable
private fun SettlementsCard(
    settlements: List<Settlement>,
    myUserId: String,
    partnerName: String,
    onUndoClick: (Settlement) -> Unit
) {
    val dateFormat = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(text = "Settle-up payments", style = MaterialTheme.typography.titleMedium)

            settlements.forEach { settlement ->
                val date = Instant.ofEpochMilli(settlement.date)
                    .atZone(ZoneId.systemDefault())
                    .format(dateFormat)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = settlementText(settlement, myUserId, partnerName),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = listOfNotNull(date, settlement.note).joinToString(" · "),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Text(
                        text = MoneyFormatter.format(settlement.amountMinor, DISPLAY_CURRENCY),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    TextButton(onClick = { onUndoClick(settlement) }) { Text("Undo") }
                }
            }
        }
    }
}

/**
 * Dialog for recording a payment: direction, amount (pre-filled with the
 * open balance; change it for a partial payment) and an optional note.
 */
@Composable
private fun SettleDialog(
    dialog: SettleDialogState,
    partnerName: String,
    onDirectionChange: (Boolean) -> Unit,
    onAmountChange: (String) -> Unit,
    onNoteChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Record a payment") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = dialog.partnerPaysMe,
                        onClick = { onDirectionChange(true) },
                        label = { Text("$partnerName paid me") }
                    )
                    FilterChip(
                        selected = !dialog.partnerPaysMe,
                        onClick = { onDirectionChange(false) },
                        label = { Text("I paid $partnerName") }
                    )
                }

                OutlinedTextField(
                    value = dialog.amountText,
                    onValueChange = onAmountChange,
                    label = { Text("Amount (EUR)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = dialog.errorMessage != null,
                    supportingText = {
                        Text(
                            dialog.errorMessage
                                ?: "Pre-filled with the open balance; change it for a partial payment."
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = dialog.note,
                    onValueChange = onNoteChange,
                    label = { Text("Note (optional)") },
                    placeholder = { Text("e.g. bank transfer") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = !dialog.isSaving) {
                Text(if (dialog.isSaving) "Saving…" else "Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
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

/** "You paid Maja" or "Maja paid you", depending on the direction. */
private fun settlementText(settlement: Settlement, myUserId: String, partnerName: String): String =
    if (settlement.fromUser == myUserId) "You paid $partnerName" else "$partnerName paid you"