package com.aljaz.spendingtracker.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.aljaz.spendingtracker.domain.model.TransactionSource
import com.aljaz.spendingtracker.domain.model.TransactionType

/**
 * One row of the "transactions" table: a single income or expense.
 *
 * Money is stored in minor units (cents) as a Long to avoid the
 * rounding errors that Float/Double cause. 12.50 EUR is stored as 1250.
 */
@Entity(
    tableName = "transactions",
    indices = [
        // Speeds up "show me this month" queries.
        Index(value = ["timestamp"]),
        // Prevents the same purchase being saved twice (e.g. SMS + Wallet).
        // Manual entries leave this null, and nulls don't count as duplicates.
        Index(value = ["dedupeHash"], unique = true)
    ]
)
data class TransactionEntity(
    /** Unique id, generated automatically by the database. */
    @PrimaryKey(autoGenerate = true) val id: Long = 0,

    /** Income or expense. */
    val type: TransactionType,

    /** Amount in minor units (cents). Always positive. */
    val amountMinor: Long,

    /** ISO 4217 currency code, e.g. "EUR". */
    val currency: String,

    /** Category this belongs to. Null until the user picks one. */
    val categoryId: Long?,

    /** Short label, e.g. the merchant name. */
    val title: String,

    /** Optional free-text note from the user. */
    val note: String?,

    /** When the transaction happened, in UTC epoch milliseconds. */
    val timestamp: Long,

    /** How this transaction entered the app. */
    val source: TransactionSource,

    /** Original SMS/notification text, kept for debugging parsers. */
    val rawText: String?,

    /** Fingerprint used to detect duplicates from automation. */
    val dedupeHash: String?,

    /** False for auto-added entries until the user confirms them. */
    val reviewed: Boolean = true,


    /** Repeating series this entry belongs to, or null for a one-off. */
    val recurringId: Long? = null
)