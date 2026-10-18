package com.aljaz.spendingtracker.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.aljaz.spendingtracker.domain.model.RepeatFrequency
import com.aljaz.spendingtracker.domain.model.TransactionType

/**
 * One row of the "recurring_transactions" table: a template that keeps
 * creating the same transaction (see the domain model RecurringTransaction).
 */
@Entity(
    tableName = "recurring_transactions",
    // Speeds up "clear this category" when a category is deleted.
    indices = [Index(value = ["categoryId"])]
)
data class RecurringTransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: TransactionType,
    /** Amount in minor units (cents). */
    val amountMinor: Long,
    val currency: String,
    val categoryId: Long?,
    val title: String,
    val note: String?,
    val frequency: RepeatFrequency,
    /** First occurrence, UTC epoch millis. */
    val startTimestamp: Long,
    /** How many occurrences have been created so far. */
    val generatedCount: Int,
    val createdAt: Long
)