package com.aljaz.spendingtracker.domain.usecase

import com.aljaz.spendingtracker.core.AppResult
import com.aljaz.spendingtracker.domain.model.Category
import com.aljaz.spendingtracker.domain.model.SyncState
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
    if (isShared) {
        if (type != TransactionType.EXPENSE) {
            return AppResult.Error("Only expenses can be split.")
        }
        if (payerSharePercent == null || payerSharePercent !in 0..100) {
            return AppResult.Error("Your share must be between 0 and 100 %.")
        }
    }

    val cleaned = copy(
        // A blank title falls back to the type name, so quick entries work.
        title = title.trim().ifEmpty {
            when (type) {
                TransactionType.INCOME -> "Income"
                TransactionType.EXPENSE -> "Expense"
            }
        },
        note = note?.trim()?.ifEmpty { null },
        // A personal transaction never carries a share.
        payerSharePercent = if (isShared) payerSharePercent else null
    )
    return AppResult.Success(cleaned)
}

/**
 * Decides whether the sync job has to upload this transaction after it is saved.
 *
 * - Shared: PENDING, so the new version is uploaded.
 * - No longer shared, but it was before (PENDING or SYNCED): still PENDING,
 *   so the sync job can remove it from the server.
 * - Never shared: LOCAL_ONLY, it never leaves the phone.
 */
internal fun Transaction.withSyncStateForSave(): Transaction =
    copy(
        syncState = if (isShared || syncState != SyncState.LOCAL_ONLY) {
            SyncState.PENDING
        } else {
            SyncState.LOCAL_ONLY
        }
    )

/**
 * Pre-fills the split fields from [category]: an expense in a split category
 * is shared with the category's default share; anything else is personal.
 */
fun Transaction.withSplitDefaults(category: Category?): Transaction =
    if (type == TransactionType.EXPENSE && category?.isSplit == true) {
        copy(isShared = true, payerSharePercent = category.defaultSplitPercent)
    } else {
        copy(isShared = false, payerSharePercent = null)
    }