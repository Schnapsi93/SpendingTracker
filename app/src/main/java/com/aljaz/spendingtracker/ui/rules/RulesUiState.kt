package com.aljaz.spendingtracker.ui.rules

import com.aljaz.spendingtracker.domain.model.Category
import com.aljaz.spendingtracker.domain.model.CategoryRule
import com.aljaz.spendingtracker.domain.model.RuleField

/**
 * What the add/edit rule dialog currently shows.
 *
 * @property existing the rule being edited, or null when creating a new one
 * @property categoryId the chosen category, or null if none is chosen yet
 * @property error a validation error to show, or null
 */
data class RuleEditorState(
    val existing: CategoryRule? = null,
    val keyword: String = "",
    val field: RuleField = RuleField.MERCHANT,
    val categoryId: Long? = null,
    val error: String? = null
) {
    /** True when creating a new rule rather than editing one. */
    val isNew: Boolean
        get() = existing == null
}

/**
 * Everything the Rules screen shows at one moment.
 *
 * @property rules all rules, alphabetical by keyword
 * @property categories all categories, for names, colors and the picker
 * @property editor the open add/edit dialog, or null
 * @property pendingDelete the rule waiting for delete confirmation, or null
 * @property isLoading true until the first data has arrived
 */
data class RulesUiState(
    val rules: List<CategoryRule> = emptyList(),
    val categories: List<Category> = emptyList(),
    val editor: RuleEditorState? = null,
    val pendingDelete: CategoryRule? = null,
    val isLoading: Boolean = true
) {
    /** Categories keyed by id, for quick lookup when drawing each rule. */
    val categoriesById: Map<Long, Category>
        get() = categories.associateBy { it.id }
}