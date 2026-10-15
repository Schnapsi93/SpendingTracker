package com.aljaz.spendingtracker.data.csv

import com.aljaz.spendingtracker.domain.model.RuleField
import com.aljaz.spendingtracker.domain.model.TransactionType
import javax.inject.Inject

/** One category read from a categories section. [colorArgb] is null if the color was invalid. */
data class CsvCategoryRow(
    val name: String,
    val type: TransactionType,
    val colorArgb: Int?
)

/** One rule read from a rules section; its category is named, not referenced by id. */
data class CsvRuleRow(
    val keyword: String,
    val field: RuleField,
    val categoryName: String,
    val categoryType: TransactionType
)

/** Rows that could be read, plus one message per row that couldn't. */
data class ParsedRows<T>(
    val rows: List<T>,
    val errors: List<String>
)

/**
 * Reads the categories and rules sections of a backup.
 * Receives the already-split section (see [splitSections]); row 0 is the header.
 */
class ConfigurationCsvParser @Inject constructor() {

    fun parseCategories(table: List<List<String>>): ParsedRows<CsvCategoryRow> {
        val rows = mutableListOf<CsvCategoryRow>()
        val errors = mutableListOf<String>()

        table.drop(1).forEachIndexed { index, fields ->
            val rowNumber = index + 1
            if (fields.size != CsvFormat.CATEGORY_HEADER.size) {
                errors.add(
                    "Row $rowNumber: expected ${CsvFormat.CATEGORY_HEADER.size} columns, found ${fields.size}"
                )
                return@forEachIndexed
            }
            val name = fields[0].trim()
            val type = CsvFormat.parseType(fields[1])
            when {
                name.isEmpty() -> errors.add("Row $rowNumber: missing name")
                type == null -> errors.add("Row $rowNumber: unknown type '${fields[1]}'")
                else -> rows.add(CsvCategoryRow(name, type, CsvFormat.parseColor(fields[2])))
            }
        }
        return ParsedRows(rows, errors)
    }

    fun parseRules(table: List<List<String>>): ParsedRows<CsvRuleRow> {
        val rows = mutableListOf<CsvRuleRow>()
        val errors = mutableListOf<String>()

        table.drop(1).forEachIndexed { index, fields ->
            val rowNumber = index + 1
            if (fields.size != CsvFormat.RULE_HEADER.size) {
                errors.add(
                    "Row $rowNumber: expected ${CsvFormat.RULE_HEADER.size} columns, found ${fields.size}"
                )
                return@forEachIndexed
            }
            val keyword = fields[0].trim()
            val field = when (fields[1].trim().lowercase()) {
                CsvFormat.FIELD_MERCHANT.lowercase() -> RuleField.MERCHANT
                CsvFormat.FIELD_OPERATION.lowercase() -> RuleField.OPERATION
                else -> null
            }
            val categoryName = fields[2].trim()
            val categoryType = CsvFormat.parseType(fields[3])
            when {
                keyword.isEmpty() -> errors.add("Row $rowNumber: missing keyword")
                field == null -> errors.add("Row $rowNumber: unknown 'Looks at' value '${fields[1]}'")
                categoryName.isEmpty() -> errors.add("Row $rowNumber: missing category")
                categoryType == null ->
                    errors.add("Row $rowNumber: unknown category type '${fields[3]}'")
                else -> rows.add(CsvRuleRow(keyword, field, categoryName, categoryType))
            }
        }
        return ParsedRows(rows, errors)
    }
}