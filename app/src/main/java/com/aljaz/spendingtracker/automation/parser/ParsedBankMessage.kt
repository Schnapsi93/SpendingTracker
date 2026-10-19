package com.aljaz.spendingtracker.automation.parser

import com.aljaz.spendingtracker.domain.model.TransactionType

/**
 * The information extracted from one bank message (SMS or app notification),
 * before it becomes a transaction (categorizing and saving happen later).
 *
 * @property operation the bank's own label, e.g. "POS NAKUP" or "BA DVIG"
 * @property amountMinor amount in minor units (cents), always positive
 * @property timestamp when the transaction happened, UTC epoch milliseconds
 * @property merchant cleaned-up merchant name, e.g. "BS LOGO BREZOVICA"
 * @property cardLast4 last four digits of the card, e.g. "1754"
 */
data class ParsedBankMessage(
    val operation: String,
    val type: TransactionType,
    val amountMinor: Long,
    val currency: String,
    val timestamp: Long,
    val merchant: String,
    val cardLast4: String
)