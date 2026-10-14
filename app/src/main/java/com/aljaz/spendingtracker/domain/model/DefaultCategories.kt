package com.aljaz.spendingtracker.domain.model

/**
 * The categories created on first launch.
 *
 * To change the starting set, edit this list. It only affects
 * databases that don't have any categories yet.
 */
object DefaultCategories {

    val all: List<Category> = listOf(
        // Expenses
        expense("Groceries", 0xFF4CAF50),
        expense("Eating out", 0xFFFF9800),
        expense("Transport", 0xFF2196F3),
        expense("Housing", 0xFF795548),
        expense("Utilities", 0xFF607D8B),
        expense("Health", 0xFFE91E63),
        expense("Shopping", 0xFF9C27B0),
        expense("Entertainment", 0xFFFFC107),
        expense("Other", 0xFF9E9E9E),
        // Income
        income("Salary", 0xFF388E3C),
        income("Gifts", 0xFF00BCD4),
        income("Other", 0xFF9E9E9E)
    )

    private fun expense(name: String, color: Long) =
        Category(name = name, type = TransactionType.EXPENSE, colorArgb = color.toInt(), isDefault = true)

    private fun income(name: String, color: Long) =
        Category(name = name, type = TransactionType.INCOME, colorArgb = color.toInt(), isDefault = true)
}