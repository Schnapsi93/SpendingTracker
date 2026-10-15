package com.aljaz.spendingtracker.domain.usecase

import com.aljaz.spendingtracker.core.AppResult
import com.aljaz.spendingtracker.domain.model.CategoryRule
import com.aljaz.spendingtracker.domain.model.RuleField
import com.aljaz.spendingtracker.domain.repository.CategoryRuleRepository
import com.aljaz.spendingtracker.domain.rules.CategoryRuleMatcher
import com.aljaz.spendingtracker.domain.rules.RuleText
import javax.inject.Inject

/**
 * Creates a keyword rule from a corrected transaction, so future
 * transactions from the same merchant are categorized automatically.
 */
class CreateRuleFromTransactionUseCase @Inject constructor(
    private val ruleRepository: CategoryRuleRepository,
    private val matcher: CategoryRuleMatcher
) {

    /**
     * @param keyword what the user typed, e.g. "BS LOGO"
     * @param merchant the transaction's merchant, e.g. "BS LOGO BREZOVICA";
     *        the keyword must match it, so the rule is known to work
     * @param categoryId the category future matches should get
     * @return [AppResult.Success] with the saved rule, or [AppResult.Error] with a reason
     */
    suspend operator fun invoke(
        keyword: String,
        merchant: String,
        categoryId: Long
    ): AppResult<CategoryRule> {
        val normalized = RuleText.normalize(keyword)
        if (normalized.length < MIN_KEYWORD_LENGTH) {
            return AppResult.Error("Enter at least $MIN_KEYWORD_LENGTH letters or digits.")
        }

        val rule = CategoryRule(
            keyword = normalized,
            field = RuleField.MERCHANT,
            categoryId = categoryId
        )

        // Guard against typos: the rule must match the merchant it was made from.
        if (matcher.match(listOf(rule), merchant) == null) {
            return AppResult.Error(
                "\"$normalized\" doesn't appear as whole word(s) in \"$merchant\"."
            )
        }

        val id = ruleRepository.add(rule)
        return AppResult.Success(rule.copy(id = id))
    }

    private companion object {
        const val MIN_KEYWORD_LENGTH = 2
    }
}