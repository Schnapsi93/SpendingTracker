package com.aljaz.spendingtracker.domain.sharing

/**
 * A settle-up payment between the two users: [fromUser] paid [toUser]
 * money to even out the shared expenses.
 *
 * @property fromUser server user id of the person who paid
 * @property toUser server user id of the person who received the money
 * @property amountMinor amount in minor units (cents)
 * @property date when it was recorded, UTC epoch milliseconds
 */
data class Settlement(
    val uuid: String,
    val fromUser: String,
    val toUser: String,
    val amountMinor: Long,
    val date: Long,
    val note: String?
)