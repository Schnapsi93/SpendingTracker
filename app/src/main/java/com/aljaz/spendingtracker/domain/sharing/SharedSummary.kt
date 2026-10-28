package com.aljaz.spendingtracker.domain.sharing

/**
 * Who paid how much in one category.
 *
 * @property category category name, as the payers named it
 */
data class SharedCategoryTotal(
    val category: String,
    val myPaidMinor: Long,
    val partnerPaidMinor: Long
) {
    val totalMinor: Long
        get() = myPaidMinor + partnerPaidMinor
}

/**
 * Totals of shared expenses over some time span, seen from this user's side.
 *
 * @property myPaidMinor what this user paid
 * @property partnerPaidMinor what the partner paid
 * @property myShareMinor this user's part of all these expenses, by the agreed shares
 * @property partnerShareMinor the partner's part of all these expenses
 * @property settledMinor settle-up payments, net: positive = this user paid the
 *           partner that much more than the other way round
 * @property balanceMinor positive: the partner owes this user that much;
 *           negative: this user owes the partner; zero: even
 */
data class SharedTotals(
    val myPaidMinor: Long = 0,
    val partnerPaidMinor: Long = 0,
    val myShareMinor: Long = 0,
    val partnerShareMinor: Long = 0,
    val settledMinor: Long = 0,
    val balanceMinor: Long = 0
) {
    val totalMinor: Long
        get() = myPaidMinor + partnerPaidMinor
}

/** Totals plus the per-category breakdown, largest category first. */
data class SharedSummary(
    val totals: SharedTotals,
    val byCategory: List<SharedCategoryTotal>
) {
    companion object {
        val EMPTY = SharedSummary(SharedTotals(), emptyList())
    }
}

/**
 * Turns a list of shared expenses (and optionally settle-up payments)
 * into a [SharedSummary].
 *
 * Pure arithmetic with no Android or database parts, so it is easy to test.
 * Every expense is split with [SplitMath], the same rounding the form uses.
 */
object SharedSummaryCalculator {

    /**
     * @param expenses the expenses to add up
     * @param myUserId this user's server id; anyone else counts as the partner
     * @param settlements settle-up payments to take into account; leave empty
     *        for a single period, pass all of them for the all-time balance
     */
    fun calculate(
        expenses: List<SharedExpense>,
        myUserId: String,
        settlements: List<Settlement> = emptyList()
    ): SharedSummary {
        var myPaid = 0L
        var partnerPaid = 0L
        var myShare = 0L
        var partnerShare = 0L

        // category name → (my paid, partner paid)
        val byCategory = linkedMapOf<String, Pair<Long, Long>>()

        for (expense in expenses) {
            val otherPart = SplitMath.partnerShareMinor(expense.amountMinor, expense.payerSharePercent)
            val payerPart = expense.amountMinor - otherPart
            val (mine, theirs) = byCategory[expense.category] ?: (0L to 0L)

            if (expense.paidBy == myUserId) {
                myPaid += expense.amountMinor
                myShare += payerPart
                partnerShare += otherPart
                byCategory[expense.category] = (mine + expense.amountMinor) to theirs
            } else {
                partnerPaid += expense.amountMinor
                partnerShare += payerPart
                myShare += otherPart
                byCategory[expense.category] = mine to (theirs + expense.amountMinor)
            }
        }

        // Money I sent counts like an expense I paid for the partner; money I
        // received lowers what the partner still owes.
        var settled = 0L
        for (settlement in settlements) {
            when (myUserId) {
                settlement.fromUser -> settled += settlement.amountMinor
                settlement.toUser -> settled -= settlement.amountMinor
            }
        }

        return SharedSummary(
            totals = SharedTotals(
                myPaidMinor = myPaid,
                partnerPaidMinor = partnerPaid,
                myShareMinor = myShare,
                partnerShareMinor = partnerShare,
                settledMinor = settled,
                // Paid more than my share → the partner owes me the difference.
                balanceMinor = myPaid - myShare + settled
            ),
            byCategory = byCategory
                .map { (name, paid) -> SharedCategoryTotal(name, paid.first, paid.second) }
                .sortedByDescending { it.totalMinor }
        )
    }
}