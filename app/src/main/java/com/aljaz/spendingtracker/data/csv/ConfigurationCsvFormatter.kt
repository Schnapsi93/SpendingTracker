package com.aljaz.spendingtracker.data.csv

import com.aljaz.spendingtracker.domain.model.Category
import com.aljaz.spendingtracker.domain.model.CategoryRule
import com.aljaz.spendingtracker.domain.model.RuleField
import javax.inject.Inject
import com.aljaz.spendingtracker.domain.model.TransactionType

/**
 * Turns categories and keyword rules into CSV text, for backups.
 * The counterpart of [ConfigurationCsvParser].
 */
class ConfigurationCsvFormatter @Inject constructor() {

    /** Categories section: one row per category, expenses first, alphabetical. */
    fun formatCategories(categories: List<Category>): String = buildString {
        append(csvRow(CsvFormat.CATEGORY_HEADER))
        categories
            // Expenses first, then income; alphabetical within each.
            // (Sorting by `type` directly would follow the enum's declaration order,
            // which lists INCOME first.)
            .sortedWith(
                compareBy(
                    { it.type != TransactionType.EXPENSE },
                    { it.name.lowercase() }
                )
            )
            .forEach { category ->
                append(
                    csvRow(
                        listOf(
                            category.name,
                            CsvFormat.formatType(category.type),
                            CsvFormat.formatColor(category.colorArgb)
                        )
                    )
                )
            }
    }

    /**
     * Rules section: one row per rule, alphabetical by keyword. Categories are
     * written by name and type (not id), so the file still works after a
     * reinstall where ids differ.
     */
    fun formatRules(rules: List<CategoryRule>, categoriesById: Map<Long, Category>): String =
        buildString {
            append(csvRow(CsvFormat.RULE_HEADER))
            rules.sortedBy { it.keyword }.forEach { rule ->
                // A rule without its category can't be restored meaningfully; skip it.
                val category = categoriesById[rule.categoryId] ?: return@forEach
                append(
                    csvRow(
                        listOf(
                            rule.keyword,
                            when (rule.field) {
                                RuleField.MERCHANT -> CsvFormat.FIELD_MERCHANT
                                RuleField.OPERATION -> CsvFormat.FIELD_OPERATION
                            },
                            category.name,
                            CsvFormat.formatType(category.type)
                        )
                    )
                )
            }
        }
}