package com.aljaz.spendingtracker.domain.model

import java.util.UUID

/**
 * A single income or expense, as the rest of the app sees it.
 *
 * This is the "clean" version of the database row: it knows nothing
 * about Room, so UI and business logic stay independent of storage.
 *
 * @property id local database id, or 0 for a transaction that is not saved yet
 * @property amountMinor amount in minor units (cents), always positive
 * @property timestamp when it happened, UTC epoch milliseconds
 * @property reviewed false for auto-added entries awaiting confirmation
 * @property recurringId the repeating series this entry belongs to, or null
 * @property uuid global id, the same on the phone and the server; a new
 *           transaction gets a random one, edits must keep it (use copy())
 * @property isShared true when the expense is split with the partner
 * @property payerSharePercent this user's share in percent when shared, else null
 * @property syncState whether the latest version still has to be uploaded
 * @property updatedAt last edit time, UTC epoch milliseconds
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
    val reviewed: Boolean = true,
    val recurringId: Long? = null,
    val uuid: String = UUID.randomUUID().toString(),
    val isShared: Boolean = false,
    val payerSharePercent: Int? = null,
    val syncState: SyncState = SyncState.LOCAL_ONLY,
    val updatedAt: Long = System.currentTimeMillis()
)