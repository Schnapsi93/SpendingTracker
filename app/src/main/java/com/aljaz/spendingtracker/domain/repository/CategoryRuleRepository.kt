package com.aljaz.spendingtracker.domain.repository

import com.aljaz.spendingtracker.domain.model.CategoryRule
import kotlinx.coroutines.flow.Flow

/** The single entry point for reading and writing category rules. */
interface CategoryRuleRepository {

    /**
     * Saves a rule. The keyword is stored in simplified form. If a rule
     * with the same keyword and field exists, it is replaced, so a
     * keyword always points to exactly one category.
     *
     * @return the id of the saved rule
     */
    suspend fun add(rule: CategoryRule): Long

    suspend fun delete(rule: CategoryRule)

    /** All rules, for matching. */
    suspend fun getAll(): List<CategoryRule>

    /** All rules, alphabetical; emits again whenever they change. */
    fun observeAll(): Flow<List<CategoryRule>>

    suspend fun count(): Int
}