package com.aljaz.spendingtracker.domain.model

/**
 * Whether a transaction brings money in or sends money out.
 *
 * The amount itself is always stored as a positive number;
 * this type decides how it affects the balance.
 */
enum class TransactionType {
    INCOME,
    EXPENSE
}