package com.aljaz.spendingtracker.domain.model

/**
 * A message from a known bank that no parser could read, kept so the
 * user can inspect it, copy it, or retry it after a parser update.
 *
 * @property receivedAt when the app received it, UTC epoch milliseconds
 * @property source how it arrived (SMS today, Wallet later)
 */
data class UnrecognizedMessage(
    val id: Long = 0,
    val sender: String,
    val body: String,
    val receivedAt: Long,
    val source: TransactionSource
)