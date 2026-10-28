package com.aljaz.spendingtracker.domain.sharing

/**
 * One shared expense of the household, paid by either user, as the
 * summary sees it. Comes from the local mirror of the server.
 *
 * @property paidBy server user id of the person who paid
 * @property amountMinor the whole expense in minor units (cents)
 * @property date when it happened, UTC epoch milliseconds
 * @property category category name, as the payer named it
 * @property payerSharePercent the payer's share; the other person owes the rest
 */
data class SharedExpense(
    val uuid: String,
    val paidBy: String,
    val amountMinor: Long,
    val currency: String,
    val date: Long,
    val category: String,
    val payerSharePercent: Int,
    val note: String?
)