package com.aljaz.spendingtracker.ui.review

import android.content.ClipData
import android.content.ClipboardManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aljaz.spendingtracker.domain.model.UnrecognizedMessage
import com.aljaz.spendingtracker.ui.components.TransactionRow
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * Review queue: automatic transactions waiting for confirmation, and
 * bank messages that couldn't be read.
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
    val context = LocalContext.current

    /** Puts a message's text on the clipboard. */
    val copyToClipboard: (String) -> Unit = { text ->
        context.getSystemService(ClipboardManager::class.java)
            ?.setPrimaryClip(ClipData.newPlainText("Bank message", text))
    }

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

            state.notice?.let { notice ->
                Text(text = notice, style = MaterialTheme.typography.bodyMedium)
            }

            if (!state.isLoading && state.isEmpty) {
                Text(
                    text = "Nothing to review.",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            }

            LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
                // ----- Automatic transactions -----
                if (state.transactions.isNotEmpty()) {
                    item(key = "transactions_hint") {
                        Text(
                            text = "Check the category and amount of each automatic transaction. " +
                                    "Tap one to change it, or ✓ to confirm it as it is.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                items(items = state.transactions, key = { "tx_${it.id}" }) { transaction ->
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

                // ----- Unrecognized messages -----
                if (state.unrecognized.isNotEmpty()) {
                    item(key = "unrecognized_header") {
                        Column(modifier = Modifier.padding(top = 24.dp, bottom = 4.dp)) {
                            Text(
                                text = "Unrecognized messages",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "These came from your bank but couldn't be read. " +
                                        "Copy one to report a new format, or try again after an update.",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
                items(items = state.unrecognized, key = { "msg_${it.id}" }) { message ->
                    UnrecognizedMessageItem(
                        message = message,
                        onCopy = { copyToClipboard(message.body) },
                        onRetry = { viewModel.onRetry(message) },
                        onDelete = { viewModel.onDelete(message) }
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

/**
 * One unreadable message: sender, time, the full text (selectable),
 * and buttons to copy, retry or delete it.
 */
@Composable
private fun UnrecognizedMessageItem(
    message: UnrecognizedMessage,
    onCopy: () -> Unit,
    onRetry: () -> Unit,
    onDelete: () -> Unit
) {
    val received = Instant.ofEpochMilli(message.receivedAt)
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT))

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "${message.sender} · $received",
            style = MaterialTheme.typography.bodySmall
        )
        // Long-press to select part of the text.
        SelectionContainer {
            Text(text = message.body, style = MaterialTheme.typography.bodyMedium)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = onCopy) { Text("Copy") }
            TextButton(onClick = onRetry) { Text("Try again") }
            TextButton(onClick = onDelete) {
                Text("Delete", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}