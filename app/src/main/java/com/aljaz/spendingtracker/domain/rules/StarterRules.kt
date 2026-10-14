package com.aljaz.spendingtracker.domain.rules

import com.aljaz.spendingtracker.domain.model.RuleField

/**
 * One rule created on first launch. The category is named rather than
 * referenced by id, because it may not exist yet.
 *
 * @property categoryColor used only if the category has to be created
 */
data class StarterRule(
    val keyword: String,
    val field: RuleField,
    val categoryName: String,
    val categoryColor: Long
)

/**
 * The rules created when the rules table is empty.
 * All are expense rules. Change the list here to change the starting set;
 * existing installs keep their own rules.
 */
object StarterRules {

    private const val HOBBIES_COLOR = 0xFF3F51B5
    private const val FUEL_COLOR = 0xFFF44336
    private const val BIKE_COLOR = 0xFF388E3C
    private const val GROCERIES_COLOR = 0xFF4CAF50
    private const val ATM_COLOR = 0xFF00BCD4

    val all: List<StarterRule> = listOf(
        StarterRule("ANTHROPIC", RuleField.MERCHANT, "Hobbies", HOBBIES_COLOR),
        StarterRule("EBAY", RuleField.MERCHANT, "Hobbies", HOBBIES_COLOR),
        StarterRule("BS LOGO", RuleField.MERCHANT, "Car / Fuel", FUEL_COLOR),
        StarterRule("PETROL", RuleField.MERCHANT, "Car / Fuel", FUEL_COLOR),
        StarterRule("EASYPARK", RuleField.MERCHANT, "Car / Fuel", FUEL_COLOR),
        StarterRule("BIKE24", RuleField.MERCHANT, "Bike / workout", BIKE_COLOR),
        StarterRule("ZET", RuleField.MERCHANT, "Bike / workout", BIKE_COLOR),
        StarterRule("SUPERMARKET", RuleField.MERCHANT, "Groceries", GROCERIES_COLOR),
        // Any ATM withdrawal, whichever bank's ATM it was.
        StarterRule("BA DVIG", RuleField.OPERATION, "ATM", ATM_COLOR)
    )
}