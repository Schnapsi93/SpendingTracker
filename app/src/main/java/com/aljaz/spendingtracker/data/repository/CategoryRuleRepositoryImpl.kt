package com.aljaz.spendingtracker.data.repository

import com.aljaz.spendingtracker.data.local.dao.CategoryRuleDao
import com.aljaz.spendingtracker.data.mapper.toDomain
import com.aljaz.spendingtracker.data.mapper.toEntity
import com.aljaz.spendingtracker.domain.model.CategoryRule
import com.aljaz.spendingtracker.domain.repository.CategoryRuleRepository
import com.aljaz.spendingtracker.domain.rules.RuleText
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/** Room-backed implementation of [CategoryRuleRepository]. */
class CategoryRuleRepositoryImpl @Inject constructor(
    private val dao: CategoryRuleDao
) : CategoryRuleRepository {

    override suspend fun add(rule: CategoryRule): Long =
        // Store keywords simplified, so "bs logo" and "BS  LOGO" count as the same rule.
        dao.insert(rule.copy(keyword = RuleText.normalize(rule.keyword)).toEntity())

    override suspend fun delete(rule: CategoryRule) =
        dao.delete(rule.toEntity())

    override suspend fun getAll(): List<CategoryRule> =
        dao.getAll().map { it.toDomain() }

    override fun observeAll(): Flow<List<CategoryRule>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun count(): Int = dao.count()
}