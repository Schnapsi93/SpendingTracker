package com.aljaz.spendingtracker.automation.parser

import com.aljaz.spendingtracker.core.MoneyParser
import java.time.ZoneId

/**
 * The "Prejeli ste" notification for incoming transfers, used by more than
 * one banking app (NLB Pay, DH Denarnik). Saved as income, with the
 * sender's name as merchant.
 *
 * Examples:
 * `22.10.2026 - Prejeli ste 3.40€ od Majda D.`
 * `23.10.2026 - Prejeli ste 5.00 € od Sabina P.`
 *
 * Only the format is shared; each bank's parser still decides which app
 * it accepts (see BankMessageParser.acceptsSender).
 */
object IncomingTransferFormat {

    /**
     * @param text the notification text
     * @param zone time zone of the date written in the message
     * @return the extracted data, or null if the text isn't in this format
     */
    fun parse(text: String, zone: ZoneId): ParsedBankMessage? {
        val groups = PATTERN.find(text.trim())?.groups ?: return null

        // "Prejeli ste" -> "PREJELI STE"; typeOf() maps it to INCOME.
        val operation = groups["operation"]!!.value.replace(Regex("\\s+"), " ").uppercase()
        val type = BankMessageFields.typeOf(operation) ?: return null

        // No time in this format, so the start of that day is used.
        val timestamp = BankMessageFields.parseDay(groups["date"]!!.value, zone) ?: return null

        // The pattern only allows "3.40" or "3,40" (no thousands separator),
        // so MoneyParser can read it directly. NOT parseAmountMinor(): that
        // treats the dot as a thousands separator and would read 340,00.
        val amountMinor = MoneyParser.parseToMinor(groups["amount"]!!.value) ?: return null

        return ParsedBankMessage(
            operation = operation,
            type = type,
            amountMinor = amountMinor,
            currency = BankMessageFields.currencyCode(groups["currency"]!!.value),
            timestamp = timestamp,
            merchant = BankMessageFields.cleanMerchant(groups["merchant"]!!.value),
            cardLast4 = null // transfers have no card
        )
    }

    /**
     * - amount: exactly two decimals after a dot or comma; anything else
     *   (e.g. "1,234.56") doesn't match and goes to unrecognized messages.
     * - \s* between amount and currency: works with "5.00€" and "5.00 €".
     * - currency: the euro sign (written as \u20AC, which the regex engine
     *   understands) or a written "EUR".
     * - merchant: the sender's name, everything after "od" to the end.
     */
    private val PATTERN = Regex(
        """^\s*(?<date>\d{1,2}\.\d{1,2}\.\d{4})\s*-\s*""" +
                """(?<operation>Prejeli\s+ste)\s+""" +
                """(?<amount>\d+[.,]\d{2})\s*(?<currency>\u20AC|EUR)\s+""" +
                """od\s+(?<merchant>.+?)\s*$""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
    )
}