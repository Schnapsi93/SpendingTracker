package com.aljaz.spendingtracker.domain.sharing

/**
 * The arithmetic of splitting one expense.
 *
 * Kept in one place so the form preview, the summary and the settlement
 * always round the same way.
 */
object SplitMath {

    /**
     * What the partner owes for an expense: the part not covered by the
     * payer's share, rounded to the nearest cent (half a cent rounds up).
     *
     * Example: 12.35 € with a payer share of 50 % → the partner owes 6.18 €.
     *
     * @param amountMinor the whole expense in minor units (cents)
     * @param payerSharePercent the payer's share, 0–100
     */
    fun partnerShareMinor(amountMinor: Long, payerSharePercent: Int): Long {
        require(payerSharePercent in 0..100) { "Share must be 0..100, was $payerSharePercent" }
        return (amountMinor * (100 - payerSharePercent) + 50) / 100
    }

    /** The payer's own part: the rest, so both parts always add up to the amount. */
    fun payerShareMinor(amountMinor: Long, payerSharePercent: Int): Long =
        amountMinor - partnerShareMinor(amountMinor, payerSharePercent)
}