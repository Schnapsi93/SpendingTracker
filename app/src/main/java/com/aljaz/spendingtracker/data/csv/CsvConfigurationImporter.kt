package com.aljaz.spendingtracker.data.csv

import com.aljaz.spendingtracker.domain.export.ImportSummary
import com.aljaz.spendingtracker.domain.model.Category
import com.aljaz.spendingtracker.domain.model.CategoryRule
import com.aljaz.spendingtracker.domain.model.TransactionType
import com.aljaz.spendingtracker.domain.repository.CategoryRepository
import com.aljaz.spendingtracker.domain.repository.CategoryRuleRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Imports the categories and keyword rules sections of a backup.
 * Only adds or updates; nothing is ever deleted.
 */
class CsvConfigurationImporter @Inject constructor(
    private val parser: ConfigurationCsvParser,
    private val categoryRepository: CategoryRepository,
    private val ruleRepository: CategoryRuleRepository
) {

    /** Adds missing categories; existing ones (same name and type) take the file's color. */
    suspend fun importCategories(section: List<List<String>>): ImportSummary {
        val parsed = parser.parseCategories(section)
        val existing = categoriesByKey()
        var added = 0
        var recolored = 0

        for (row in parsed.rows) {
            val key = categoryKey(row.type, row.name)
            val current = existing[key]
            if (current == null) {
                val category = Category(
                    name = row.name,
                    type = row.type,
                    colorArgb = row.colorArgb ?: NEW_CATEGORY_COLOR
                )
                categoryRepository.add(category)?.let { id ->
                    existing[key] = category.copy(id = id)
                    added++
                }
            } else if (row.colorArgb != null && row.colorArgb != current.colorArgb) {
                if (categoryRepository.update(current.copy(colorArgb = row.colorArgb))) recolored++
            }
        }

        return ImportSummary(
            categoriesAdded = added,
            categoriesRecolored = recolored,
            errors = parsed.errors.map { "Categories, $it" }
        )
    }

    /**
     * Adds each rule (replacing a rule with the same keyword), creating
     * the category it points to if it doesn't exist yet.
     */
    suspend fun importRules(section: List<List<String>>): ImportSummary {
        val parsed = parser.parseRules(section)
        val categories = categoriesByKey()
        var saved = 0
        var created = 0

        for (row in parsed.rows) {
            val key = categoryKey(row.categoryType, row.categoryName)
            val category = categories[key]
                ?: createCategory(row.categoryName, row.categoryType)?.also {
                    categories[key] = it
                    created++
                }
                ?: continue // couldn't create the category; skip this rule

            ruleRepository.add(
                CategoryRule(keyword = row.keyword, field = row.field, categoryId = category.id)
            )
            saved++
        }

        return ImportSummary(
            rulesImported = saved,
            categoriesCreated = created,
            errors = parsed.errors.map { "Rules, $it" }
        )
    }

    /** Creates a grey category; returns it with its new id, or null. */
    private suspend fun createCategory(name: String, type: TransactionType): Category? {
        val category = Category(name = name, type = type, colorArgb = NEW_CATEGORY_COLOR)
        return categoryRepository.add(category)?.let { id -> category.copy(id = id) }
    }

    /** Existing categories keyed by type + lowercase name. */
    private suspend fun categoriesByKey(): MutableMap<String, Category> =
        categoryRepository.observeAll().first()
            .associateBy { categoryKey(it.type, it.name) }
            .toMutableMap()

    private fun categoryKey(type: TransactionType, name: String): String =
        "${type.name}|${name.trim().lowercase()}"

    private companion object {
        /** Neutral grey for categories created without a color. */
        val NEW_CATEGORY_COLOR: Int = 0xFF9E9E9E.toInt()
    }
}