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
 * All are expense rules, and their categories match [com.aljaz.spendingtracker.domain.model.DefaultCategories],
 * so on a fresh install no extra categories are created.
 * Change the list here to change the starting set; existing instals keep their own rules.
 */
object StarterRules {

    // Same names and colors as in DefaultCategories.
    private const val BIKE = "bajk / telovadba"
    private const val BIKE_COLOR = 0xFF2196F3
    private const val FUEL = "fuel, avto servis"
    private const val FUEL_COLOR = 0xFFF44336
    private const val HOBBIES = "hobbies"
    private const val HOBBIES_COLOR = 0xFF3F51B5
    private const val FOOD = "fooder"
    private const val FOOD_COLOR = 0xFF4CAF50
    private const val OTHER = "razno"
    private const val OTHER_COLOR = 0xFF9E9E9E

    val all: List<StarterRule> = listOf(
        StarterRule("ANTHROPIC", RuleField.MERCHANT, HOBBIES, HOBBIES_COLOR),
        StarterRule("EBAY", RuleField.MERCHANT, HOBBIES, HOBBIES_COLOR),
        StarterRule("BS LOGO", RuleField.MERCHANT, FUEL, FUEL_COLOR),
        StarterRule("PETROL", RuleField.MERCHANT, FUEL, FUEL_COLOR),
        StarterRule("EASYPARK", RuleField.MERCHANT, FUEL, FUEL_COLOR),
        StarterRule("BIKE24", RuleField.MERCHANT, BIKE, BIKE_COLOR),
        StarterRule("ZET", RuleField.MERCHANT, BIKE, BIKE_COLOR),
        StarterRule("SUPERMARKET", RuleField.MERCHANT, FOOD, FOOD_COLOR),
        // Any ATM withdrawal, whichever bank's ATM it was.
        StarterRule("BA DVIG", RuleField.OPERATION, OTHER, OTHER_COLOR)
    )
}