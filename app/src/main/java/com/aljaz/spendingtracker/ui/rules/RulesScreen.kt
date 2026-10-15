package com.aljaz.spendingtracker.ui.rules

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExtendedFloatingActionButton
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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aljaz.spendingtracker.domain.model.Category
import com.aljaz.spendingtracker.domain.model.CategoryRule
import com.aljaz.spendingtracker.domain.model.RuleField
import com.aljaz.spendingtracker.ui.components.CategorySelector
import com.aljaz.spendingtracker.ui.components.ColorDot

/**
 * Screen listing all keyword rules. Rules can be added, edited and deleted here.
 *
 * @param onBackClick called when the user wants to go back
 * @param viewModel provided by Hilt automatically
 */
@Composable
fun RulesScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RulesViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        modifier = modifier,
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = viewModel::onAddClick) {
                Text("Add rule")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onBackClick) { Text("< Back") }
                Text(text = "Keyword rules", style = MaterialTheme.typography.titleLarge)
            }

            Text(
                text = "Bank transactions whose merchant contains a keyword get its category " +
                        "automatically. Keywords match whole words, ignoring case.",
                style = MaterialTheme.typography.bodyMedium
            )

            if (!state.isLoading && state.rules.isEmpty()) {
                Text(
                    text = "No rules yet.",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            }

            LazyColumn(
                // Extra bottom space so the button never covers the last row.
                contentPadding = PaddingValues(bottom = 88.dp)
            ) {
                items(items = state.rules, key = { it.id }) { rule ->
                    RuleRow(
                        rule = rule,
                        category = state.categoriesById[rule.categoryId],
                        onClick = { viewModel.onRuleClick(rule) }
                    )
                    HorizontalDivider()
                }
            }
        }
    }

    // Add/edit dialog, shown while the ViewModel has an open editor.
    state.editor?.let { editor ->
        RuleEditorDialog(
            editor = editor,
            categories = state.categories,
            onKeywordChange = viewModel::onKeywordChange,
            onFieldChange = viewModel::onFieldChange,
            onCategoryClick = viewModel::onCategoryClick,
            onSave = viewModel::onSaveClick,
            onDelete = viewModel::onDeleteClick,
            onDismiss = viewModel::onEditorDismiss
        )
    }

    // Delete confirmation.
    state.pendingDelete?.let { rule ->
        AlertDialog(
            onDismissRequest = viewModel::onDeleteDismiss,
            title = { Text("Delete rule \"${rule.keyword}\"?") },
            text = { Text("Transactions already saved keep their category.") },
            confirmButton = {
                TextButton(onClick = viewModel::onDeleteConfirm) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = viewModel::onDeleteDismiss) { Text("Cancel") }
            }
        )
    }
}

/**
 * One rule in the list: category color, keyword, and where it leads.
 *
 * @param category the rule's category, or null if it can't be found
 */
@Composable
private fun RuleRow(
    rule: CategoryRule,
    category: Category?,
    onClick: () -> Unit
) {
    val dotColor = category?.colorArgb
        ?: MaterialTheme.colorScheme.outlineVariant.toArgb()
    val target = category?.name ?: "Unknown category"
    val fieldNote = if (rule.field == RuleField.OPERATION) " · operation" else ""

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ColorDot(colorArgb = dotColor)
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(text = rule.keyword, style = MaterialTheme.typography.bodyLarge)
            Text(text = "→ $target$fieldNote", style = MaterialTheme.typography.bodySmall)
        }
    }
}

/** Dialog for creating or editing a rule. */
@Composable
private fun RuleEditorDialog(
    editor: RuleEditorState,
    categories: List<Category>,
    onKeywordChange: (String) -> Unit,
    onFieldChange: (RuleField) -> Unit,
    onCategoryClick: (Long) -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (editor.isNew) "New rule" else "Edit rule") },
        text = {
            // Scrollable, because the category chips can make the dialog tall.
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = editor.keyword,
                    onValueChange = onKeywordChange,
                    label = { Text("Keyword") },
                    singleLine = true,
                    isError = editor.error != null,
                    supportingText = editor.error?.let { message -> { Text(message) } },
                    modifier = Modifier.fillMaxWidth()
                )

                Text(text = "Looks at", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = editor.field == RuleField.MERCHANT,
                        onClick = { onFieldChange(RuleField.MERCHANT) },
                        label = { Text("Merchant") }
                    )
                    FilterChip(
                        selected = editor.field == RuleField.OPERATION,
                        onClick = { onFieldChange(RuleField.OPERATION) },
                        label = { Text("Operation") }
                    )
                }
                if (editor.field == RuleField.OPERATION) {
                    Text(
                        text = "Operation rules match the bank's label, e.g. \"BA DVIG\" " +
                                "for ATM withdrawals, and win over merchant rules.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Text(text = "Category", style = MaterialTheme.typography.labelLarge)
                CategorySelector(
                    categories = categories,
                    selectedId = editor.categoryId,
                    onCategoryClick = onCategoryClick
                )

                // Only existing rules can be deleted.
                if (!editor.isNew) {
                    TextButton(onClick = onDelete) {
                        Text("Delete rule", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onSave) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}