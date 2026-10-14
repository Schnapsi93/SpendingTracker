package com.aljaz.spendingtracker.domain.usecase

import com.aljaz.spendingtracker.core.AppResult
import com.aljaz.spendingtracker.domain.model.Transaction
import com.aljaz.spendingtracker.domain.model.TransactionType

/**
 * Rules every transaction must pass before it is saved.
 *
 * Used by both adding and updating, so the two can never drift apart.
 *
 * @return [AppResult.Success] with a cleaned-up copy (trimmed text,
 *         default title), or [AppResult.Error] explaining what is wrong
 */
internal fun Transaction.validateAndClean(): AppResult<Transaction> {
    if (amountMinor <= 0) {
        return AppResult.Error("Amount must be greater than zero.")
    }

    val cleaned = copy(
        // A blank title falls back to the type name, so quick entries work.
        title = title.trim().ifEmpty {
            when (type) {
                TransactionType.INCOME -> "Income"
                TransactionType.EXPENSE -> "Expense"
            }
        },
        note = note?.trim()?.ifEmpty { null }
    )
    return AppResult.Success(cleaned)
}