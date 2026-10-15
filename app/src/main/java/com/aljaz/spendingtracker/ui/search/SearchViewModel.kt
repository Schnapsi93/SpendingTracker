package com.aljaz.spendingtracker.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aljaz.spendingtracker.domain.model.CategoryFilter
import com.aljaz.spendingtracker.domain.model.TransactionFilter
import com.aljaz.spendingtracker.domain.model.TransactionType
import com.aljaz.spendingtracker.domain.usecase.ObserveCategoriesUseCase
import com.aljaz.spendingtracker.domain.usecase.SearchTransactionsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject

/**
 * Logic behind the Search screen: holds the filter and streams the
 * matching transactions.
 */
@HiltViewModel
class SearchViewModel @Inject constructor(
    searchTransactions: SearchTransactionsUseCase,
    observeCategories: ObserveCategoriesUseCase
) : ViewModel() {

    private val _filter = MutableStateFlow(TransactionFilter())

    /**
     * The current filter. Exposed directly (not through [uiState]) so the
     * search field updates instantly while typing.
     */
    val filter: StateFlow<TransactionFilter> = _filter.asStateFlow()

    /** Results update by themselves when the filter or the data changes. */
    val uiState: StateFlow<SearchUiState> = combine(
        searchTransactions(_filter),
        observeCategories()
    ) { results, categories ->
        SearchUiState(results = results, categories = categories, isLoading = false)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SearchUiState()
    )

    fun onQueryChange(query: String) =
        _filter.update { it.copy(query = query) }

    /**
     * Changes the type filter. A chosen category of the other type
     * would give no results, so it is reset to "All categories".
     */
    fun onTypeChange(type: TransactionType?) {
        val categories = uiState.value.categories
        _filter.update { current ->
            val category = current.category
            val stillFits = type == null ||
                    category !is CategoryFilter.Specific ||
                    categories.any { it.id == category.categoryId && it.type == type }
            current.copy(
                type = type,
                category = if (stillFits) category else CategoryFilter.AllCategories
            )
        }
    }

    fun onCategoryFilterChange(category: CategoryFilter) =
        _filter.update { it.copy(category = category) }

    /** Removes all filters. */
    fun onClearClick() {
        _filter.value = TransactionFilter()
    }
}