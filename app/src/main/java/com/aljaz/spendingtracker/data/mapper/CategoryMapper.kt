package com.aljaz.spendingtracker.data.mapper

import com.aljaz.spendingtracker.data.local.entity.CategoryEntity
import com.aljaz.spendingtracker.domain.model.Category

/** Converts a database row into the domain model. */
fun CategoryEntity.toDomain(): Category = Category(
    id = id,
    name = name,
    type = type,
    colorArgb = colorArgb,
    isDefault = isDefault
)

/** Converts the domain model into a database row. */
fun Category.toEntity(): CategoryEntity = CategoryEntity(
    id = id,
    name = name,
    type = type,
    colorArgb = colorArgb,
    isDefault = isDefault
)