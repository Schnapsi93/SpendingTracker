package com.aljaz.spendingtracker.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.aljaz.spendingtracker.domain.model.TransactionType

/**
 * One row of the "categories" table.
 *
 * The unique index stops two categories with the same name and type,
 * e.g. two expense categories both called "Groceries".
 */
@Entity(
    tableName = "categories",
    indices = [Index(value = ["name", "type"], unique = true)]
)
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: TransactionType,
    val colorArgb: Int,
    val isDefault: Boolean
)