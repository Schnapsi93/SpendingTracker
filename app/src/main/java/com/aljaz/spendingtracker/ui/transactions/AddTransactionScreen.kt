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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.aljaz.spendingtracker.ui.components.DateSelectDialog
import com.aljaz.spendingtracker.ui.components.TimeSelectDialog
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Checkbox
import androidx.compose.ui.Alignment
import androidx.compose.ui.semantics.Role
import com.aljaz.spendingtracker.domain.model.RepeatFrequency
import com.aljaz.spendingtracker.core.MoneyFormatter

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


    // Which picker dialog is open. Purely visual state, so it lives here.
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var showTimePicker by rememberSaveable { mutableStateOf(false) }

    // The transaction's moment in the phone's time zone, for display and the pickers.
    val dateTime = Instant.ofEpochMilli(state.timestamp).atZone(ZoneId.systemDefault())

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

            Text(text = "Date & time", style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { showDatePicker = true }) {
                    Text(dateTime.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)))
                }
                OutlinedButton(onClick = { showTimePicker = true }) {
                    Text(dateTime.format(DateTimeFormatter.ofPattern("HH:mm")))
                }
            }

            // "Repeating transaction" tick box; frequency chips appear once ticked.
            RepeatSection(
                repeats = state.repeats,
                frequency = state.frequency,
                onRepeatChange = viewModel::onRepeatChange,
                onFrequencyChange = viewModel::onFrequencyChange
            )

            Text(text = "Category", style = MaterialTheme.typography.titleSmall)
            CategorySelector(
                categories = state.categoriesForType,
                selectedId = state.selectedCategoryId,
                onCategoryClick = viewModel::onCategoryClick
            )

            // Only for expenses in a split category.
            if (state.canSplit) {
                SplitSection(
                    isShared = state.isShared,
                    percentText = state.splitPercentText,
                    partnerShareText = state.partnerShareMinor?.let {
                        MoneyFormatter.format(it, "EUR")
                    },
                    errorMessage = state.splitErrorMessage,
                    onSplitChange = viewModel::onSplitChange,
                    onPercentChange = viewModel::onSplitPercentChange
                )
            }

            // Only for automatic transactions with a category chosen.
            if (state.canCreateRule) {
                OutlinedButton(onClick = viewModel::onApplyToFutureClick) {
                    Text("Apply to all future transactions…")
                }
            }

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

            // "Apply to all future transactions" dialog.
            state.ruleDialog?.let { dialog ->
                AlertDialog(
                    onDismissRequest = viewModel::onRuleDialogDismiss,
                    title = { Text("Apply to future transactions") },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                text = "Future bank transactions whose merchant contains this " +
                                        "keyword will go to ${state.selectedCategoryName ?: "this category"}.",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            OutlinedTextField(
                                value = dialog.keyword,
                                onValueChange = viewModel::onRuleKeywordChange,
                                label = { Text("Keyword") },
                                singleLine = true,
                                isError = dialog.error != null,
                                supportingText = {
                                    Text(
                                        dialog.error
                                            ?: "Shorten it to match more places, e.g. \"BS LOGO\" " +
                                            "instead of \"BS LOGO BREZOVICA\"."
                                    )
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = viewModel::onRuleConfirm) { Text("Create rule and save") }
                    },
                    dismissButton = {
                        TextButton(onClick = viewModel::onRuleDialogDismiss) { Text("Cancel") }
                    }
                )
            }

            // Date and time pickers.
            if (showDatePicker) {
                DateSelectDialog(
                    initialDate = dateTime.toLocalDate(),
                    onDateSelected = { date ->
                        viewModel.onDateSelected(date)
                        showDatePicker = false
                    },
                    onDismiss = { showDatePicker = false }
                )
            }
            if (showTimePicker) {
                TimeSelectDialog(
                    initialTime = dateTime.toLocalTime(),
                    onTimeSelected = { time ->
                        viewModel.onTimeSelected(time)
                        showTimePicker = false
                    },
                    onDismiss = { showTimePicker = false }
                )
            }
        }
    }
}


/**
 * Tick box for a repeating transaction and, when ticked, the choice of
 * how often it repeats.
 *
 * The whole row is clickable, not just the small box, which is easier to hit.
 */
@Composable
private fun RepeatSection(
    repeats: Boolean,
    frequency: RepeatFrequency,
    onRepeatChange: (Boolean) -> Unit,
    onFrequencyChange: (RepeatFrequency) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .toggleable(
                    value = repeats,
                    onValueChange = onRepeatChange,
                    role = Role.Checkbox
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // onCheckedChange is null: the row above handles the click.
            Checkbox(checked = repeats, onCheckedChange = null)
            Text(
                text = "Repeating transaction",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(start = 12.dp)
            )
        }

        if (repeats) {
            Text(text = "Repeats", style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RepeatFrequency.entries.forEach { option ->
                    FilterChip(
                        selected = frequency == option,
                        onClick = { onFrequencyChange(option) },
                        label = { Text(option.label) }
                    )
                }
            }
            Text(
                text = "Repeats from the date above. Future entries use what you save here. " +
                        "Untick and save to stop; entries already added stay.",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}


/**
 * "Split this between users" tick box and, when ticked, this user's share
 * with a live preview of what the partner owes.
 *
 * @param partnerShareText the partner's part, formatted, or null while the
 *        amount or the share can't be read yet
 */
@Composable
private fun SplitSection(
    isShared: Boolean,
    percentText: String,
    partnerShareText: String?,
    errorMessage: String?,
    onSplitChange: (Boolean) -> Unit,
    onPercentChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .toggleable(
                    value = isShared,
                    onValueChange = onSplitChange,
                    role = Role.Checkbox
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // onCheckedChange is null: the row above handles the click.
            Checkbox(checked = isShared, onCheckedChange = null)
            Text(
                text = "Split this between users",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(start = 12.dp)
            )
        }

        if (isShared) {
            OutlinedTextField(
                value = percentText,
                onValueChange = onPercentChange,
                label = { Text("Your share (%)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isError = errorMessage != null,
                supportingText = {
                    Text(
                        errorMessage
                            ?: partnerShareText?.let { "Partner owes you $it" }
                            ?: "The default comes from the category."
                    )
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}