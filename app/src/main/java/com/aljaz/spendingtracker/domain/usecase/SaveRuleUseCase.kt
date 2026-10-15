package com.aljaz.spendingtracker.domain.usecase

import com.aljaz.spendingtracker.core.AppResult
import com.aljaz.spendingtracker.domain.model.CategoryRule
import com.aljaz.spendingtracker.domain.repository.CategoryRuleRepository
import com.aljaz.spendingtracker.domain.rules.RuleText
import javax.inject.Inject

/**
 * Validates and saves a keyword rule created or edited by hand.
 * A rule with id 0 is added; otherwise the existing rule is replaced.
 */
class SaveRuleUseCase @Inject constructor(
    private val ruleRepository: CategoryRuleRepository
) {

    /** @return [AppResult.Success] when saved, or [AppResult.Error] with a reason */
    suspend operator fun invoke(rule: CategoryRule): AppResult<Unit> {
        val keyword = RuleText.normalize(rule.keyword)
        if (keyword.length < MIN_KEYWORD_LENGTH) {
            return AppResult.Error("Enter at least $MIN_KEYWORD_LENGTH letters or digits.")
        }
        ruleRepository.add(rule.copy(keyword = keyword))
        return AppResult.Success(Unit)
    }

    private companion object {
        const val MIN_KEYWORD_LENGTH = 2
    }
}