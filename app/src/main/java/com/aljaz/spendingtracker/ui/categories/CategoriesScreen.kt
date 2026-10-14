package com.aljaz.spendingtracker.ui.categories

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aljaz.spendingtracker.domain.model.Category
import com.aljaz.spendingtracker.domain.model.TransactionType
import com.aljaz.spendingtracker.ui.components.ColorDot

/**
 * Screen listing all categories, split into expenses and income.
 * Categories can be added, renamed, recolored and deleted here.
 *
 * @param onBackClick called when the user wants to return to Home
 * @param viewModel provided by Hilt automatically
 */
@Composable
fun CategoriesScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CategoriesViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        modifier = modifier,
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = viewModel::onAddClick) {
                Text("Add category")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onBackClick) { Text("< Back") }
                Text(text = "Categories", style = MaterialTheme.typography.titleLarge)
            }

            LazyColumn(
                // Extra bottom space so the button never covers the last row.
                contentPadding = PaddingValues(bottom = 88.dp)
            ) {
                categorySection("Expenses", state.expenseCategories, viewModel::onCategoryClick)
                categorySection("Income", state.incomeCategories, viewModel::onCategoryClick)
            }
        }
    }

    // Add/edit dialog, shown while the ViewModel has an open editor.
    state.editor?.let { editor ->
        CategoryEditorDialog(
            editor = editor,
            onNameChange = viewModel::onNameChange,
            onTypeChange = viewModel::onTypeChange,
            onColorChange = viewModel::onColorChange,
            onSave = viewModel::onSaveClick,
            onDelete = viewModel::onDeleteClick,
            onDismiss = viewModel::onEditorDismiss
        )
    }

    // Delete confirmation.
    state.pendingDelete?.let { category ->
        AlertDialog(
            onDismissRequest = viewModel::onDeleteDismiss,
            title = { Text("Delete \"${category.name}\"?") },
            text = { Text("Its transactions are kept and become uncategorized.") },
            confirmButton = {
                TextButton(onClick = viewModel::onDeleteConfirm) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = viewModel::onDeleteDismiss) { Text("Cancel") }
            }
        )
    }
}

/** Adds a section title followed by one row per category to the list. */
private fun LazyListScope.categorySection(
    title: String,
    categories: List<Category>,
    onCategoryClick: (Category) -> Unit
) {
    item(key = "header_$title") {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
        )
    }
    items(items = categories, key = { it.id }) { category ->
        CategoryRow(category = category, onClick = { onCategoryClick(category) })
        HorizontalDivider()
    }
}

/** One category in the list: color dot and name. Tapping opens the editor. */
@Composable
private fun CategoryRow(category: Category, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ColorDot(colorArgb = category.colorArgb)
        Spacer(modifier = Modifier.width(12.dp))
        Text(text = category.name, style = MaterialTheme.typography.bodyLarge)
    }
}

/**
 * Dialog for creating or editing a category.
 * The type can only be chosen for new categories.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategoryEditorDialog(
    editor: CategoryEditorState,
    onNameChange: (String) -> Unit,
    onTypeChange: (TransactionType) -> Unit,
    onColorChange: (Int) -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (editor.isNew) "New category" else "Edit category") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = editor.name,
                    onValueChange = onNameChange,
                    label = { Text("Name") },
                    singleLine = true,
                    isError = editor.errorMessage != null,
                    supportingText = editor.errorMessage?.let { message -> { Text(message) } },
                    modifier = Modifier.fillMaxWidth()
                )

                if (editor.isNew) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TransactionType.entries.forEach { type ->
                            FilterChip(
                                selected = editor.type == type,
                                onClick = { onTypeChange(type) },
                                label = {
                                    Text(type.name.lowercase().replaceFirstChar { it.uppercase() })
                                }
                            )
                        }
                    }
                }

                Text(text = "Color", style = MaterialTheme.typography.labelLarge)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CategoryPalette.colors.forEach { color ->
                        ColorSwatch(
                            colorArgb = color,
                            selected = color == editor.colorArgb,
                            onClick = { onColorChange(color) }
                        )
                    }
                }

                // Only existing categories can be deleted.
                if (!editor.isNew) {
                    TextButton(onClick = onDelete) {
                        Text("Delete category", color = MaterialTheme.colorScheme.error)
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

/** A tappable color circle; the selected one gets a ring around it. */
@Composable
private fun ColorSwatch(colorArgb: Int, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(Color(colorArgb))
            .then(
                if (selected) {
                    Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                } else {
                    Modifier
                }
            )
            .clickable(onClick = onClick)
    )
}