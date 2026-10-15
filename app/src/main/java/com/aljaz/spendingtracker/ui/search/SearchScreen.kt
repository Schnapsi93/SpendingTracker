package com.aljaz.spendingtracker.ui.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aljaz.spendingtracker.core.MoneyFormatter
import com.aljaz.spendingtracker.domain.model.Category
import com.aljaz.spendingtracker.domain.model.CategoryFilter
import com.aljaz.spendingtracker.domain.model.TransactionFilter
import com.aljaz.spendingtracker.domain.model.TransactionType
import com.aljaz.spendingtracker.ui.components.ColorDot
import com.aljaz.spendingtracker.ui.components.TransactionRow

/** Currency used for the totals until multi-currency support exists. */
private const val DISPLAY_CURRENCY = "EUR"

/**
 * Search across all transactions by text, amount, type and category.
 *
 * @param onBackClick called when the user wants to return to Home
 * @param onTransactionClick called with a transaction's id to open its edit form
 * @param viewModel provided by Hilt automatically
 */
@Composable
fun SearchScreen(
    onBackClick: () -> Unit,
    onTransactionClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SearchViewModel = hiltViewModel()
) {
    val filter by viewModel.filter.collectAsState()
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
                    text = "Search",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f)
                )
                if (!filter.isEmpty) {
                    TextButton(onClick = viewModel::onClearClick) { Text("Clear") }
                }
            }

            OutlinedTextField(
                value = filter.query,
                onValueChange = viewModel::onQueryChange,
                label = { Text("Search") },
                placeholder = { Text("Merchant, note or amount") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            TypeChips(selected = filter.type, onTypeChange = viewModel::onTypeChange)

            CategoryChips(
                filter = filter,
                categories = state.categories,
                onCategoryFilterChange = viewModel::onCategoryFilterChange
            )

            // e.g. "12 transactions · Expenses €345.20 · Income €0.00"
            if (!state.isLoading) {
                Text(
                    text = "${state.results.size} transactions · " +
                            "Expenses ${MoneyFormatter.format(state.expenseTotalMinor, DISPLAY_CURRENCY)} · " +
                            "Income ${MoneyFormatter.format(state.incomeTotalMinor, DISPLAY_CURRENCY)}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
                items(items = state.results, key = { it.id }) { transaction ->
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

/** All / Expenses / Income selector. */
@Composable
private fun TypeChips(
    selected: TransactionType?,
    onTypeChange: (TransactionType?) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(
            selected = selected == null,
            onClick = { onTypeChange(null) },
            label = { Text("All") }
        )
        FilterChip(
            selected = selected == TransactionType.EXPENSE,
            onClick = { onTypeChange(TransactionType.EXPENSE) },
            label = { Text("Expenses") }
        )
        FilterChip(
            selected = selected == TransactionType.INCOME,
            onClick = { onTypeChange(TransactionType.INCOME) },
            label = { Text("Income") }
        )
    }
}

/**
 * Sideways-scrolling category chips: All categories, Uncategorized, then
 * every category that fits the chosen type.
 */
@Composable
private fun CategoryChips(
    filter: TransactionFilter,
    categories: List<Category>,
    onCategoryFilterChange: (CategoryFilter) -> Unit
) {
    val visible = if (filter.type == null) categories else categories.filter { it.type == filter.type }
    val selectedId = (filter.category as? CategoryFilter.Specific)?.categoryId

    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        item(key = "all") {
            FilterChip(
                selected = filter.category == CategoryFilter.AllCategories,
                onClick = { onCategoryFilterChange(CategoryFilter.AllCategories) },
                label = { Text("All categories") }
            )
        }
        item(key = "uncategorized") {
            FilterChip(
                selected = filter.category == CategoryFilter.Uncategorized,
                onClick = { onCategoryFilterChange(CategoryFilter.Uncategorized) },
                label = { Text("Uncategorized") }
            )
        }
        items(items = visible, key = { it.id }) { category ->
            FilterChip(
                selected = category.id == selectedId,
                onClick = { onCategoryFilterChange(CategoryFilter.Specific(category.id)) },
                label = { Text(category.name) },
                leadingIcon = { ColorDot(category.colorArgb) }
            )
        }
    }
}