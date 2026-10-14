package com.aljaz.spendingtracker.data.mapper

import com.aljaz.spendingtracker.data.local.entity.CategoryRuleEntity
import com.aljaz.spendingtracker.domain.model.CategoryRule

/** Converts a database row into the domain model. */
fun CategoryRuleEntity.toDomain(): CategoryRule = CategoryRule(
    id = id,
    keyword = keyword,
    field = field,
    categoryId = categoryId,
    createdAt = createdAt
)

/** Converts the domain model into a database row. */
fun CategoryRule.toEntity(): CategoryRuleEntity = CategoryRuleEntity(
    id = id,
    keyword = keyword,
    field = field,
    categoryId = categoryId,
    createdAt = createdAt
)