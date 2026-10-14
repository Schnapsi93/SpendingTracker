package com.aljaz.spendingtracker.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.aljaz.spendingtracker.domain.model.RuleField

/**
 * One row of the "category_rules" table.
 *
 * The unique index makes each keyword point to exactly one category
 * (per field); the second index speeds up "delete this category's rules".
 */
@Entity(
    tableName = "category_rules",
    indices = [
        Index(value = ["field", "keyword"], unique = true),
        Index(value = ["categoryId"])
    ]
)
data class CategoryRuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val keyword: String,
    val field: RuleField,
    val categoryId: Long,
    val createdAt: Long
)