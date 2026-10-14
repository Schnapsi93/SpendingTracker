package com.aljaz.spendingtracker.data.repository

import androidx.room.withTransaction
import com.aljaz.spendingtracker.data.local.AppDatabase
import com.aljaz.spendingtracker.data.local.dao.CategoryDao
import com.aljaz.spendingtracker.data.local.dao.TransactionDao
import com.aljaz.spendingtracker.data.mapper.toDomain
import com.aljaz.spendingtracker.data.mapper.toEntity
import com.aljaz.spendingtracker.domain.model.Category
import com.aljaz.spendingtracker.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import com.aljaz.spendingtracker.data.local.dao.CategoryRuleDao

/** Room-backed implementation of [CategoryRepository]. */
class CategoryRepositoryImpl @Inject constructor(
    private val database: AppDatabase,
    private val categoryDao: CategoryDao,
    private val transactionDao: TransactionDao,
    private val ruleDao: CategoryRuleDao
) : CategoryRepository {

    override suspend fun add(category: Category): Long? {
        val rowId = categoryDao.insert(category.toEntity())
        return if (rowId == -1L) null else rowId
    }

    override suspend fun addAll(categories: List<Category>) =
        categoryDao.insertAll(categories.map { it.toEntity() })

    override suspend fun update(category: Category): Boolean =
        categoryDao.update(category.toEntity()) > 0

    override suspend fun delete(category: Category) {
        // All steps run as one database transaction: either all happen or
        // none do, so nothing is ever left pointing at a missing category.
        database.withTransaction {
            transactionDao.clearCategory(category.id)
            ruleDao.deleteForCategory(category.id)
            categoryDao.delete(category.toEntity())
        }
    }
    override suspend fun getById(id: Long): Category? =
        categoryDao.getById(id)?.toDomain()

    override fun observeAll(): Flow<List<Category>> =
        categoryDao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun count(): Int = categoryDao.count()
}