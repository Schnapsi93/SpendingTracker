package com.aljaz.spendingtracker

import com.aljaz.spendingtracker.core.AppResult
import com.aljaz.spendingtracker.domain.model.CategoryRule
import com.aljaz.spendingtracker.domain.repository.CategoryRuleRepository
import com.aljaz.spendingtracker.domain.usecase.SaveRuleUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Checks saving rules created or edited by hand. */
class SaveRuleUseCaseTest {

    /** Keeps rules in memory instead of the database. */
    private class FakeRuleRepository : CategoryRuleRepository {
        val rules = mutableListOf<CategoryRule>()
        override suspend fun add(rule: CategoryRule): Long {
            rules.removeAll { it.id != 0L && it.id == rule.id } // replace when editing
            rules.add(rule)
            return rule.id
        }
        override suspend fun delete(rule: CategoryRule) {
            rules.remove(rule)
        }
        override suspend fun getAll(): List<CategoryRule> = rules
        override fun observeAll(): Flow<List<CategoryRule>> = flowOf(rules)
        override suspend fun count(): Int = rules.size
    }

    private val repository = FakeRuleRepository()
    private val saveRule = SaveRuleUseCase(repository)

    @Test
    fun savesTheKeywordInSimplifiedForm() = runBlocking {
        val result = saveRule(CategoryRule(keyword = "  lidl-brezovica ", categoryId = 4))

        assertTrue(result is AppResult.Success)
        assertEquals("LIDL BREZOVICA", repository.rules.single().keyword)
    }

    @Test
    fun editingKeepsTheRuleIdAndCreationTime() = runBlocking {
        val original = CategoryRule(id = 7, keyword = "LIDL", categoryId = 4, createdAt = 100)
        repository.rules.add(original)

        saveRule(original.copy(categoryId = 1))

        val saved = repository.rules.single()
        assertEquals(7L, saved.id)
        assertEquals(100L, saved.createdAt)
        assertEquals(1L, saved.categoryId)
    }

    @Test
    fun rejectsATooShortKeyword() = runBlocking {
        val result = saveRule(CategoryRule(keyword = "x", categoryId = 4))

        assertTrue(result is AppResult.Error)
        assertTrue(repository.rules.isEmpty())
    }
}