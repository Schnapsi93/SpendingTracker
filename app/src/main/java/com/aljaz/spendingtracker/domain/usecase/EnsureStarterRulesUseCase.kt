package com.aljaz.spendingtracker.domain.usecase

import com.aljaz.spendingtracker.domain.model.Category
import com.aljaz.spendingtracker.domain.model.CategoryRule
import com.aljaz.spendingtracker.domain.model.TransactionType
import com.aljaz.spendingtracker.domain.repository.CategoryRepository
import com.aljaz.spendingtracker.domain.repository.CategoryRuleRepository
import com.aljaz.spendingtracker.domain.rules.StarterRule
import com.aljaz.spendingtracker.domain.rules.StarterRules
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Creates the starter rules if there are no rules yet, creating any
 * categories they need. Safe to run on every app start.
 */
class EnsureStarterRulesUseCase @Inject constructor(
    private val ruleRepository: CategoryRuleRepository,
    private val categoryRepository: CategoryRepository
) {

    suspend operator fun invoke() {
        if (ruleRepository.count() > 0) return

        // Existing expense categories by lowercase name, e.g. "groceries".
        val categories = categoryRepository.observeAll().first()
            .filter { it.type == TransactionType.EXPENSE }
            .associateBy { it.name.trim().lowercase() }
            .toMutableMap()

        for (starter in StarterRules.all) {
            val key = starter.categoryName.lowercase()
            val categoryId = categories[key]?.id
                ?: createCategory(starter, categories)
                ?: continue // couldn't create it; skip this rule

            ruleRepository.add(
                CategoryRule(keyword = starter.keyword, field = starter.field, categoryId = categoryId)
            )
        }
    }

    /** Creates the starter's category and remembers it; returns its id, or null. */
    private suspend fun createCategory(
        starter: StarterRule,
        categories: MutableMap<String, Category>
    ): Long? {
        val category = Category(
            name = starter.categoryName,
            type = TransactionType.EXPENSE,
            colorArgb = starter.categoryColor.toInt()
        )
        val id = categoryRepository.add(category) ?: return null
        categories[starter.categoryName.lowercase()] = category.copy(id = id)
        return id
    }
}