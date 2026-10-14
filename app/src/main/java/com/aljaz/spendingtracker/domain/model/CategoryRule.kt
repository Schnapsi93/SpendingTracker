package com.aljaz.spendingtracker.domain.model

/** Which part of a bank message a rule looks at. */
enum class RuleField {
    /** The merchant name, e.g. "BS LOGO BREZOVICA". */
    MERCHANT,

    /** The bank's operation label, e.g. "BA DVIG" for ATM withdrawals. */
    OPERATION
}

/**
 * "When [keyword] appears in [field], use category [categoryId]."
 *
 * @property id database id, or 0 for a rule that is not saved yet
 * @property keyword stored in simplified form (see RuleText.normalize), e.g. "BS LOGO"
 * @property createdAt when the rule was made; the newer rule wins a tie
 */
data class CategoryRule(
    val id: Long = 0,
    val keyword: String,
    val field: RuleField = RuleField.MERCHANT,
    val categoryId: Long,
    val createdAt: Long = System.currentTimeMillis()
)