package com.aljaz.spendingtracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One row of the "pending_deletions" table: a "tombstone" left behind when a
 * transaction that may be on the server is deleted on this phone.
 *
 * The sync job marks the server copy as deleted, then removes the tombstone.
 */
@Entity(tableName = "pending_deletions")
data class PendingDeletionEntity(
    /** The deleted transaction's uuid. */
    @PrimaryKey val uuid: String,

    /** When it was deleted, UTC epoch milliseconds. */
    val createdAt: Long
)