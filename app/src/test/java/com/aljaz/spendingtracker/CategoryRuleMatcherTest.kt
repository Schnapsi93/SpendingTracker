package com.aljaz.spendingtracker

import com.aljaz.spendingtracker.domain.model.CategoryRule
import com.aljaz.spendingtracker.domain.model.RuleField
import com.aljaz.spendingtracker.domain.rules.CategoryRuleMatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Checks which category the rules pick for real merchant names. */
class CategoryRuleMatcherTest {

    private val matcher = CategoryRuleMatcher()

    // Category ids used in these tests.
    private val hobbies = 1L
    private val fuel = 2L
    private val bike = 3L
    private val groceries = 4L
    private val atm = 5L

    private fun rule(
        keyword: String,
        categoryId: Long,
        field: RuleField = RuleField.MERCHANT,
        createdAt: Long = 0L
    ) = CategoryRule(keyword = keyword, field = field, categoryId = categoryId, createdAt = createdAt)

    /** The same rules as StarterRules, with fixed category ids. */
    private val starterRules = listOf(
        rule("ANTHROPIC", hobbies),
        rule("EBAY", hobbies),
        rule("BS LOGO", fuel),
        rule("PETROL", fuel),
        rule("EASYPARK", fuel),
        rule("BIKE24", bike),
        rule("ZET", bike),
        rule("SUPERMARKET", groceries),
        rule("BA DVIG", atm, RuleField.OPERATION)
    )

    @Test
    fun categorizesAllRealMerchants() {
        val expected = listOf(
            "ANTHROPIC CLAUDE SUB" to hobbies,
            "BS LOGO BREZOVICA" to fuel,
            "PETROL BS LJUBLJANA" to fuel,
            "eBay O*19-15244-76499" to hobbies,
            "Bike24 GmbH" to bike,
            "EasyPark SI" to fuel,
            "SUPERMARKET BREZOVICA" to groceries,
            "ZET" to bike
        )
        expected.forEach { (merchant, categoryId) ->
            assertEquals(
                merchant, // shown if this line fails
                categoryId,
                matcher.match(starterRules, merchant, "POS NAKUP")?.categoryId
            )
        }
    }

    @Test
    fun atmWithdrawalUsesOperationRule() {
        val result = matcher.match(starterRules, "OTP BANKA D.D.", "BA DVIG")
        assertEquals(atm, result?.categoryId)
    }

    @Test
    fun operationRuleBeatsMerchantRule() {
        val rules = starterRules + rule("OTP", groceries)
        assertEquals(atm, matcher.match(rules, "OTP BANKA D.D.", "BA DVIG")?.categoryId)
    }

    @Test
    fun matchesWholeWordsOnly() {
        assertNull(matcher.match(starterRules, "ZETA SHOP", "POS NAKUP"))
        assertNull(matcher.match(starterRules, "PETROLEJ TRGOVINA", "POS NAKUP"))
    }

    @Test
    fun longerKeywordWins() {
        val rules = listOf(rule("PETROL", fuel), rule("PETROL SHOP", groceries))
        assertEquals(groceries, matcher.match(rules, "PETROL SHOP LJUBLJANA")?.categoryId)
    }

    @Test
    fun newerRuleWinsATie() {
        val rules = listOf(
            rule("LIDL", groceries, createdAt = 1),
            rule("LIDL", hobbies, createdAt = 2)
        )
        assertEquals(hobbies, matcher.match(rules, "LIDL BREZOVICA")?.categoryId)
    }

    @Test
    fun ignoresCaseAndPunctuationInKeywords() {
        val rules = listOf(rule("  bs-logo ", fuel))
        assertEquals(fuel, matcher.match(rules, "BS LOGO BREZOVICA")?.categoryId)
    }

    @Test
    fun returnsNullForUnknownMerchant() {
        assertNull(matcher.match(starterRules, "NEW SHOP D.O.O.", "POS NAKUP"))
    }
}