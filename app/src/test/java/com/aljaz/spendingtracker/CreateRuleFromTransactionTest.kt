package com.aljaz.spendingtracker

import com.aljaz.spendingtracker.core.AppResult
import com.aljaz.spendingtracker.domain.model.CategoryRule
import com.aljaz.spendingtracker.domain.repository.CategoryRuleRepository
import com.aljaz.spendingtracker.domain.rules.CategoryRuleMatcher
import com.aljaz.spendingtracker.domain.usecase.CreateRuleFromTransactionUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Checks rule creation from a corrected transaction. */
class CreateRuleFromTransactionTest {

    /** Keeps rules in memory instead of the database. */
    private class FakeRuleRepository : CategoryRuleRepository {
        val rules = mutableListOf<CategoryRule>()
        override suspend fun add(rule: CategoryRule): Long {
            rules.add(rule)
            return rules.size.toLong()
        }
        override suspend fun delete(rule: CategoryRule) {
            rules.remove(rule)
        }
        override suspend fun getAll(): List<CategoryRule> = rules
        override fun observeAll(): Flow<List<CategoryRule>> = flowOf(rules)
        override suspend fun count(): Int = rules.size
    }

    private val repository = FakeRuleRepository()
    private val createRule = CreateRuleFromTransactionUseCase(repository, CategoryRuleMatcher())

    @Test
    fun savesAShortenedKeyword() = runBlocking {
        val result = createRule("bs logo", "BS LOGO BREZOVICA", categoryId = 2)

        assertTrue(result is AppResult.Success)
        assertEquals("BS LOGO", repository.rules.single().keyword)
        assertEquals(2L, repository.rules.single().categoryId)
    }

    @Test
    fun rejectsAKeywordThatDoesNotMatchTheMerchant() = runBlocking {
        val result = createRule("LOGOS", "BS LOGO BREZOVICA", categoryId = 2)

        assertTrue(result is AppResult.Error)
        assertTrue(repository.rules.isEmpty())
    }

    @Test
    fun rejectsATooShortKeyword() = runBlocking {
        val result = createRule(" - ", "BS LOGO BREZOVICA", categoryId = 2)

        assertTrue(result is AppResult.Error)
        assertTrue(repository.rules.isEmpty())
    }
}