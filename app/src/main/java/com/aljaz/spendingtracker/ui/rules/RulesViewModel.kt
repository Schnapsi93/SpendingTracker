package com.aljaz.spendingtracker.ui.rules

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aljaz.spendingtracker.core.AppResult
import com.aljaz.spendingtracker.domain.model.CategoryRule
import com.aljaz.spendingtracker.domain.model.RuleField
import com.aljaz.spendingtracker.domain.repository.CategoryRuleRepository
import com.aljaz.spendingtracker.domain.usecase.ObserveCategoriesUseCase
import com.aljaz.spendingtracker.domain.usecase.SaveRuleUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Logic behind the Rules screen: lists keyword rules and lets the user
 * add, edit and delete them.
 */
@HiltViewModel
class RulesViewModel @Inject constructor(
    private val ruleRepository: CategoryRuleRepository,
    observeCategories: ObserveCategoriesUseCase,
    private val saveRule: SaveRuleUseCase
) : ViewModel() {

    /** The open add/edit dialog, or null. */
    private val editor = MutableStateFlow<RuleEditorState?>(null)

    /** The rule waiting for delete confirmation, or null. */
    private val pendingDelete = MutableStateFlow<CategoryRule?>(null)

    /** Updates by itself whenever rules, categories or dialogs change. */
    val uiState: StateFlow<RulesUiState> = combine(
        ruleRepository.observeAll(),
        observeCategories(),
        editor,
        pendingDelete
    ) { rules, categories, currentEditor, currentDelete ->
        RulesUiState(
            rules = rules,
            categories = categories,
            editor = currentEditor,
            pendingDelete = currentDelete,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = RulesUiState()
    )

    /** Opens an empty dialog for a new rule. */
    fun onAddClick() {
        editor.value = RuleEditorState()
    }

    /** Opens the dialog pre-filled with an existing rule. */
    fun onRuleClick(rule: CategoryRule) {
        editor.value = RuleEditorState(
            existing = rule,
            keyword = rule.keyword,
            field = rule.field,
            categoryId = rule.categoryId
        )
    }

    /** Closes the dialog without saving. */
    fun onEditorDismiss() {
        editor.value = null
    }

    fun onKeywordChange(value: String) =
        editor.update { it?.copy(keyword = value, error = null) }

    fun onFieldChange(field: RuleField) =
        editor.update { it?.copy(field = field) }

    fun onCategoryClick(categoryId: Long) =
        editor.update { it?.copy(categoryId = categoryId, error = null) }

    /** Validates and saves; closes the dialog on success, shows the error otherwise. */
    fun onSaveClick() {
        val current = editor.value ?: return
        val categoryId = current.categoryId
        if (categoryId == null) {
            editor.update { it?.copy(error = "Choose a category.") }
            return
        }

        // Editing keeps the rule's id and creation time; only the content changes.
        val rule = current.existing?.copy(
            keyword = current.keyword,
            field = current.field,
            categoryId = categoryId
        ) ?: CategoryRule(
            keyword = current.keyword,
            field = current.field,
            categoryId = categoryId
        )

        viewModelScope.launch {
            when (val result = saveRule(rule)) {
                is AppResult.Success -> editor.value = null
                is AppResult.Error -> editor.update { it?.copy(error = result.message) }
            }
        }
    }

    /** Closes the editor and asks for delete confirmation. */
    fun onDeleteClick() {
        val existing = editor.value?.existing ?: return
        editor.value = null
        pendingDelete.value = existing
    }

    /** Cancels the delete. */
    fun onDeleteDismiss() {
        pendingDelete.value = null
    }

    /** Deletes the rule after the user confirmed. */
    fun onDeleteConfirm() {
        val rule = pendingDelete.value ?: return
        pendingDelete.value = null
        viewModelScope.launch { ruleRepository.delete(rule) }
    }
}