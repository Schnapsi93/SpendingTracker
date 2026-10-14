package com.aljaz.spendingtracker.ui.transactions

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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aljaz.spendingtracker.domain.model.TransactionType
import com.aljaz.spendingtracker.ui.components.CategorySelector

/**
 * Form for adding a new transaction or editing an existing one.
 *
 * It only draws the state from [AddTransactionViewModel] and forwards
 * user input back to it. It contains no business logic.
 *
 * @param onBackClick called when the screen should close: the user
 *        cancelled, or a save or delete finished
 * @param viewModel provided by Hilt automatically
 */
@Composable
fun AddTransactionScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AddTransactionViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    // Close the screen once a save or delete has finished.
    LaunchedEffect(state.isFinished) {
        if (state.isFinished) onBackClick()
    }

    if (state.showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = viewModel::onDeleteDismiss,
            title = { Text("Delete transaction?") },
            text = { Text("This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = viewModel::onDeleteConfirm) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = viewModel::onDeleteDismiss) { Text("Cancel") }
            }
        )
    }

    Scaffold(modifier = modifier) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = if (state.isEditing) "Edit transaction" else "Add transaction",
                style = MaterialTheme.typography.headlineSmall
            )

            // Expense / Income selector
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TransactionType.entries.forEach { type ->
                    FilterChip(
                        selected = state.type == type,
                        onClick = { viewModel.onTypeChange(type) },
                        label = {
                            Text(type.name.lowercase().replaceFirstChar { it.uppercase() })
                        }
                    )
                }
            }

            OutlinedTextField(
                value = state.amountText,
                onValueChange = viewModel::onAmountChange,
                label = { Text("Amount (EUR)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                isError = state.errorMessage != null,
                supportingText = state.errorMessage?.let { message -> { Text(message) } },
                modifier = Modifier.fillMaxWidth()
            )
            Text(text = "Category", style = MaterialTheme.typography.titleSmall)
            CategorySelector(
                categories = state.categoriesForType,
                selectedId = state.selectedCategoryId,
                onCategoryClick = viewModel::onCategoryClick
            )

            OutlinedTextField(
                value = state.title,
                onValueChange = viewModel::onTitleChange,
                label = { Text("Title (optional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = state.note,
                onValueChange = viewModel::onNoteChange,
                label = { Text("Note (optional)") },
                modifier = Modifier.fillMaxWidth()
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onBackClick) {
                    Text("Cancel")
                }
                Button(
                    onClick = viewModel::onSaveClick,
                    enabled = !state.isSaving
                ) {
                    Text("Save")
                }
            }

            // Only existing transactions can be deleted.
            if (state.isEditing) {
                TextButton(
                    onClick = viewModel::onDeleteClick,
                    enabled = !state.isSaving
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}