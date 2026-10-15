package com.aljaz.spendingtracker.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.aljaz.spendingtracker.domain.model.TransactionSource

/**
 * One row of the "unrecognized_messages" table.
 * The unique index stores the same message from the same sender only once.
 */
@Entity(
    tableName = "unrecognized_messages",
    indices = [Index(value = ["sender", "body"], unique = true)]
)
data class UnrecognizedMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sender: String,
    val body: String,
    val receivedAt: Long,
    val source: TransactionSource
)