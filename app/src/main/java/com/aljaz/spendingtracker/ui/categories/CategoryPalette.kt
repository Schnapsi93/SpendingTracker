package com.aljaz.spendingtracker.ui.categories

/**
 * Colors offered when creating or editing a category.
 * Add or reorder colors here; existing categories keep their color.
 */
object CategoryPalette {

    val colors: List<Int> = listOf(
        0xFF4CAF50, // green
        0xFF388E3C, // dark green
        0xFF00BCD4, // cyan
        0xFF2196F3, // blue
        0xFF3F51B5, // indigo
        0xFF9C27B0, // purple
        0xFFE91E63, // pink
        0xFFF44336, // red
        0xFFFF9800, // orange
        0xFFFFC107, // amber
        0xFF795548, // brown
        0xFF607D8B, // blue grey
        0xFF9E9E9E  // grey
    ).map { it.toInt() }
}