package com.aljaz.spendingtracker.domain.model

/**
 * The categories created on first launch.
 *
 * To change the starting set, edit this list. It only affects
 * databases that don't have any categories yet (a fresh install).
 */
object DefaultCategories {

    val all: List<Category> = listOf(
        // Expenses
        expense("bajk / telovadba", 0xFF2196F3),   // blue
        expense("fuel, avto servis", 0xFFF44336),  // red
        expense("hobbies", 0xFF3F51B5),            // indigo
        expense("pjace vecerje", 0xFFFF9800),      // orange
        expense("fooder", 0xFF4CAF50),             // green
        expense("razno", 0xFF9E9E9E),              // grey
        expense("holidays", 0xFF00BCD4),           // cyan
        expense("rent, bills", 0xFF795548),        // brown
        expense("saprat", 0xFF9C27B0),             // purple
        expense("hausehold", 0xFFFFC107),          // amber
        expense("zdravstvo", 0xFFE91E63),          // pink
        expense("equalize", 0xFF607D8B),           // blue grey
        // Income
        income("drugo", 0xFF9E9E9E),               // grey
        income("salary", 0xFF388E3C),              // dark green
        income("equalize", 0xFF607D8B)             // blue grey
    )

    private fun expense(name: String, color: Long) =
        Category(name = name, type = TransactionType.EXPENSE, colorArgb = color.toInt(), isDefault = true)

    private fun income(name: String, color: Long) =
        Category(name = name, type = TransactionType.INCOME, colorArgb = color.toInt(), isDefault = true)
}