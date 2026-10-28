package com.aljaz.spendingtracker.domain.model

/**
 * A category such as "Groceries" or "Salary".
 *
 * Each category belongs to one [TransactionType], so the form only
 * offers expense categories for expenses and income categories for income.
 *
 * @property id database id, or 0 for a category that is not saved yet
 * @property colorArgb display color as an ARGB integer, e.g. 0xFF4CAF50
 * @property isDefault true for the categories the app created itself
 * @property isSplit true when expenses in this category are shared with the
 *           partner; only expense categories can be split
 * @property defaultSplitPercent the payer's share (0–100) pre-filled for new
 *           split expenses; can still be changed per expense
 */
data class Category(
    val id: Long = 0,
    val name: String,
    val type: TransactionType,
    val colorArgb: Int,
    val isDefault: Boolean = false,
    val isSplit: Boolean = false,
    val defaultSplitPercent: Int = 50
)