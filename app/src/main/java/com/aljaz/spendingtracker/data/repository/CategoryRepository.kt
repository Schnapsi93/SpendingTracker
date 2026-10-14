package com.aljaz.spendingtracker.domain.repository

import com.aljaz.spendingtracker.domain.model.Category
import kotlinx.coroutines.flow.Flow

/** The single entry point for reading and writing categories. */
interface CategoryRepository {

    /** @return the new id, or null if the name already exists for that type */
    suspend fun add(category: Category): Long?

    /** Adds several categories, silently skipping existing ones. */
    suspend fun addAll(categories: List<Category>)

    /** @return true when saved, false if the name already exists for that type */
    suspend fun update(category: Category): Boolean

    /**
     * Deletes a category. Its transactions are kept and become
     * uncategorized; they are never deleted.
     */
    suspend fun delete(category: Category)

    suspend fun getById(id: Long): Category?

    /** Streams all categories; emits again whenever they change. */
    fun observeAll(): Flow<List<Category>>

    /** Number of categories stored. */
    suspend fun count(): Int
}