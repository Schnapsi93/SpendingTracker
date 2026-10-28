package com.aljaz.spendingtracker.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One row of the "settlements" table: a local copy (mirror) of a settle-up
 * payment on the server. Filled only from the server; never edited by hand.
 */
@Entity(
    tableName = "settlements",
    indices = [Index(value = ["date"])]
)
data class SettlementEntity(
    /** Global id, created by the phone that recorded the payment. */
    @PrimaryKey val uuid: String,

    /** The record id on the server. */
    val remoteId: String,

    val householdId: String,

    /** Server user id of the person who paid the money. */
    val fromUser: String,

    /** Server user id of the person who received it. */
    val toUser: String,

    /** Amount in minor units (cents). */
    val amountMinor: Long,

    /** When the payment was recorded, UTC epoch milliseconds. */
    val date: Long,

    val note: String?,

    /** The server's last-change time, as text. */
    val serverUpdated: String
)