package com.aljaz.spendingtracker.domain.rules

import com.aljaz.spendingtracker.domain.model.CategoryRule
import com.aljaz.spendingtracker.domain.model.RuleField
import javax.inject.Inject

/**
 * Picks the rule that applies to a bank message.
 *
 * Keywords match whole words only, ignoring case and punctuation.
 * When several rules match, the winner is chosen in this order:
 * 1. operation rules beat merchant rules,
 * 2. the longest (most specific) keyword wins,
 * 3. the newest rule wins.
 */
class CategoryRuleMatcher @Inject constructor() {

    /**
     * @param merchant the merchant name from the message
     * @param operation the bank's operation label, if known
     * @return the winning rule, or null if no rule matches
     */
    fun match(
        rules: List<CategoryRule>,
        merchant: String,
        operation: String? = null
    ): CategoryRule? {
        // Spaces around the text let " KEYWORD " match whole words only.
        val merchantText = " ${RuleText.normalize(merchant)} "
        val operationText = operation?.let { " ${RuleText.normalize(it)} " }

        return rules
            .filter { rule ->
                val keyword = RuleText.normalize(rule.keyword)
                keyword.isNotEmpty() && when (rule.field) {
                    RuleField.MERCHANT -> merchantText.contains(" $keyword ")
                    RuleField.OPERATION -> operationText?.contains(" $keyword ") == true
                }
            }
            .maxWithOrNull(
                compareBy<CategoryRule>(
                    { it.field == RuleField.OPERATION },
                    { RuleText.normalize(it.keyword).length },
                    { it.createdAt }
                )
            )
    }
}