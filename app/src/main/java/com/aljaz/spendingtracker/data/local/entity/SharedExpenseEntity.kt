package com.aljaz.spendingtracker.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One row of the "shared_expenses" table: a local copy (mirror) of a shared
 * expense on the server, paid by either user of the household.
 *
 * Filled only by the sync job; never edited by hand. Your own split
 * expenses also live in "transactions"; this table is what the shared
 * summary reads, so both users see exactly the same numbers.
 */
@Entity(
    tableName = "shared_expenses",
    indices = [Index(value = ["date"])]
)
data class SharedExpenseEntity(
    /** Global id, the same as the payer's transaction uuid. */
    @PrimaryKey val uuid: String,

    /** The record id on the server. */
    val remoteId: String,

    val householdId: String,

    /** Server user id of the person who paid. */
    val paidBy: String,

    /** Amount in minor units (cents). */
    val amountMinor: Long,

    val currency: String,

    /** When the expense happened, UTC epoch milliseconds. */
    val date: Long,

    /** Category name, as the payer named it. */
    val category: String,

    /** The payer's share in percent; the partner owes the rest. */
    val payerSharePercent: Int,

    val note: String?,

    /** The server's last-change time, as text, e.g. "2026-10-28 16:40:12.345Z". */
    val serverUpdated: String
)