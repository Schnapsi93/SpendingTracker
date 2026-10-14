package com.aljaz.spendingtracker.domain.model

/**
 * A single income or expense, as the rest of the app sees it.
 *
 * This is the "clean" version of the database row: it knows nothing
 * about Room, so UI and business logic stay independent of storage.
 *
 * @property id database id, or 0 for a transaction that is not saved yet
 * @property amountMinor amount in minor units (cents), always positive
 * @property timestamp when it happened, UTC epoch milliseconds
 * @property reviewed false for auto-added entries awaiting confirmation
 */
data class Transaction(
    val id: Long = 0,
    val type: TransactionType,
    val amountMinor: Long,
    val currency: String,
    val categoryId: Long? = null,
    val title: String,
    val note: String? = null,
    val timestamp: Long,
    val source: TransactionSource = TransactionSource.MANUAL,
    val rawText: String? = null,
    val dedupeHash: String? = null,
    val reviewed: Boolean = true
)