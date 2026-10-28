package com.aljaz.spendingtracker.ui.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aljaz.spendingtracker.core.AppResult
import com.aljaz.spendingtracker.domain.model.Category
import com.aljaz.spendingtracker.domain.model.TransactionType
import com.aljaz.spendingtracker.domain.usecase.DeleteCategoryUseCase
import com.aljaz.spendingtracker.domain.usecase.ObserveCategoriesUseCase
import com.aljaz.spendingtracker.domain.usecase.SaveCategoryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Logic behind the Categories screen: the category list, the add/edit
 * dialog (including the split settings), and delete confirmation.
 */
@HiltViewModel
class CategoriesViewModel @Inject constructor(
    observeCategories: ObserveCategoriesUseCase,
    private val saveCategory: SaveCategoryUseCase,
    private val deleteCategory: DeleteCategoryUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(CategoriesUiState())
    val uiState: StateFlow<CategoriesUiState> = _uiState.asStateFlow()

    init {
        // Keep both lists up to date; they refresh after every save or delete.
        viewModelScope.launch {
            observeCategories().collect { list ->
                _uiState.update {
                    it.copy(
                        expenseCategories = list.filter { c -> c.type == TransactionType.EXPENSE },
                        incomeCategories = list.filter { c -> c.type == TransactionType.INCOME }
                    )
                }
            }
        }
    }

    /** Opens an empty dialog for a new category. */
    fun onAddClick() =
        _uiState.update { it.copy(editor = CategoryEditorState()) }

    /** Opens the dialog pre-filled with an existing category. */
    fun onCategoryClick(category: Category) = _uiState.update {
        it.copy(
            editor = CategoryEditorState(
                existing = category,
                name = category.name,
                type = category.type,
                colorArgb = category.colorArgb,
                isSplit = category.isSplit,
                splitPercentText = category.defaultSplitPercent.toString()
            )
        )
    }

    /** Closes the dialog without saving. */
    fun onEditorDismiss() =
        _uiState.update { it.copy(editor = null) }

    fun onNameChange(name: String) =
        updateEditor { it.copy(name = name, errorMessage = null) }

    /** Only used for new categories; existing ones keep their type. */
    fun onTypeChange(type: TransactionType) =
        updateEditor { it.copy(type = type) }

    fun onColorChange(colorArgb: Int) =
        updateEditor { it.copy(colorArgb = colorArgb) }

    /** Turns "Split with partner" on or off. */
    fun onSplitChange(isSplit: Boolean) =
        updateEditor { it.copy(isSplit = isSplit, splitErrorMessage = null) }

    /** Accepts digits only, at most 3 (so "100" fits). */
    fun onSplitPercentChange(text: String) {
        val digits = text.filter { it.isDigit() }.take(3)
        updateEditor { it.copy(splitPercentText = digits, splitErrorMessage = null) }
    }

    /** Validates and saves; closes the dialog on success, shows the error otherwise. */
    fun onSaveClick() {
        val editor = _uiState.value.editor ?: return
        val split = editor.isSplit && editor.canSplit

        // The share only matters when splitting; otherwise keep the stored value.
        val percent = if (split) {
            editor.splitPercentText.toIntOrNull()?.takeIf { it in 0..100 }
                ?: run {
                    updateEditor { it.copy(splitErrorMessage = "Enter a number from 0 to 100.") }
                    return
                }
        } else {
            editor.existing?.defaultSplitPercent ?: DEFAULT_SPLIT_PERCENT
        }

        // Editing keeps id, type and isDefault; name, color and split settings change.
        val category = editor.existing?.copy(
            name = editor.name,
            colorArgb = editor.colorArgb,
            isSplit = split,
            defaultSplitPercent = percent
        ) ?: Category(
            name = editor.name,
            type = editor.type,
            colorArgb = editor.colorArgb,
            isSplit = split,
            defaultSplitPercent = percent
        )

        viewModelScope.launch {
            when (val result = saveCategory(category)) {
                is AppResult.Success ->
                    _uiState.update { it.copy(editor = null) }
                is AppResult.Error ->
                    updateEditor { it.copy(errorMessage = result.message) }
            }
        }
    }

    /** Closes the editor and asks for delete confirmation. */
    fun onDeleteClick() {
        val existing = _uiState.value.editor?.existing ?: return
        _uiState.update { it.copy(editor = null, pendingDelete = existing) }
    }

    /** Cancels the delete. */
    fun onDeleteDismiss() =
        _uiState.update { it.copy(pendingDelete = null) }

    /** Deletes the category after the user confirmed. */
    fun onDeleteConfirm() {
        val category = _uiState.value.pendingDelete ?: return
        _uiState.update { it.copy(pendingDelete = null) }
        viewModelScope.launch { deleteCategory(category) }
    }

    /** Applies a change to the open editor; does nothing if it is closed. */
    private fun updateEditor(change: (CategoryEditorState) -> CategoryEditorState) =
        _uiState.update { state ->
            state.editor?.let { state.copy(editor = change(it)) } ?: state
        }

    private companion object {
        const val DEFAULT_SPLIT_PERCENT = 50
    }
}