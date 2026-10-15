package com.aljaz.spendingtracker.ui.review

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.aljaz.spendingtracker.ui.components.TransactionRow

/**
 * Review queue: automatic transactions waiting for the user's confirmation.
 *
 * @param onBackClick called when the user wants to return to Home
 * @param onTransactionClick called with a transaction's id to open its edit form
 * @param viewModel provided by Hilt automatically
 */
@Composable
fun ReviewScreen(
    onBackClick: () -> Unit,
    onTransactionClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ReviewViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(modifier = modifier) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onBackClick) { Text("< Back") }
                Text(
                    text = "To review",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f)
                )
                if (state.transactions.isNotEmpty()) {
                    TextButton(onClick = viewModel::onConfirmAll) { Text("Confirm all") }
                }
            }

            Text(
                text = "Check the category and amount of each automatic transaction. " +
                        "Tap one to change it, or ✓ to confirm it as it is.",
                style = MaterialTheme.typography.bodyMedium
            )

            if (!state.isLoading && state.transactions.isEmpty()) {
                Text(
                    text = "Nothing to review.",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            }

            LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
                items(items = state.transactions, key = { it.id }) { transaction ->
                    TransactionRow(
                        transaction = transaction,
                        category = transaction.categoryId?.let { state.categoriesById[it] },
                        onClick = { onTransactionClick(transaction.id) },
                        showNewBadge = false, // everything here is new
                        trailingContent = {
                            TextButton(onClick = { viewModel.onConfirm(transaction) }) {
                                Text("✓")
                            }
                        }
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}