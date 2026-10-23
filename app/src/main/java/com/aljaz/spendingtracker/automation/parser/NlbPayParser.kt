package com.aljaz.spendingtracker.automation.parser

import com.aljaz.spendingtracker.core.MoneyParser
import java.time.ZoneId
import javax.inject.Inject

/**
 * Parser for notifications from the NLB Pay app.
 *
 * Supported formats:
 *
 * Card payment:
 * `POS NAKUP 19.10.2026 11:52, kartica ***5802, znesek 10,96 EUR,
 *  CHUTIS LJUBLJANA SI. Info: 0038614772000. NLB`
 *
 * Incoming transfer (saved as income, the sender's name as merchant):
 * `22.10.2026 - Prejeli ste 3.40€ od Majda D.`
 */
class NlbPayParser @Inject constructor() : BankMessageParser {

    override val name: String = "NLB (NLB Pay notification)"

    /** For notifications the sender is the posting app's package name. */
    override fun acceptsSender(sender: String): Boolean =
        sender == PACKAGE_NAME

    /** Tries each known format in turn; null if none of them matches. */
    override fun parse(text: String, zone: ZoneId): ParsedBankMessage? {
        val trimmed = text.trim()
        return parseCardPayment(trimmed, zone)
            ?: parseIncomingTransfer(trimmed, zone)
    }

    /** "POS NAKUP 19.10.2026 11:52, kartica ***5802, znesek 10,96 EUR, ..." */
    private fun parseCardPayment(text: String, zone: ZoneId): ParsedBankMessage? {
        val groups = CARD_PATTERN.find(text)?.groups ?: return null

        val operation = groups["operation"]!!.value.trim().uppercase()
        val type = BankMessageFields.typeOf(operation) ?: return null // unknown: don't guess
        val timestamp = BankMessageFields.parseDate(groups["date"]!!.value, zone) ?: return null
        val amountMinor = BankMessageFields.parseAmountMinor(groups["amount"]!!.value) ?: return null

        return ParsedBankMessage(
            operation = operation,
            type = type,
            amountMinor = amountMinor,
            currency = BankMessageFields.currencyCode(groups["currency"]!!.value),
            timestamp = timestamp,
            merchant = BankMessageFields.cleanMerchant(groups["merchant"]!!.value),
            cardLast4 = groups["card"]!!.value
        )
    }

    /** "22.10.2026 - Prejeli ste 3.40€ od Majda D." */
    private fun parseIncomingTransfer(text: String, zone: ZoneId): ParsedBankMessage? {
        val groups = TRANSFER_PATTERN.find(text)?.groups ?: return null

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

    companion object {
        /** Package name of the NLB Pay app (from its Play Store link). */
        const val PACKAGE_NAME = "si.nlbpay.slovenija"

        /**
         * Card payments, with the same named groups as DhSmsParser.
         *
         * - operation: everything before the date that isn't a digit or comma,
         *   e.g. "POS NAKUP"; typeOf() then decides if it's known.
         * - \s+ and \s* also match line breaks, so a wrapped notification works.
         * - merchant: lazy (.+?) up to ". Info:", so names with dots stay complete.
         */
        private val CARD_PATTERN = Regex(
            """^\s*(?<operation>[^,\d]+?)\s+""" +
                    """(?<date>\d{1,2}\.\d{1,2}\.\d{4}\s+\d{1,2}:\d{2}),""" +
                    """\s*kartica\s*\*+(?<card>\d{4}),""" +
                    """\s*znesek\s+(?<amount>[\d.]+,\d{2})\s+(?<currency>[A-Za-z]{3}),""" +
                    """\s*(?<merchant>.+?)\.\s*Info:""",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
        )

        /**
         * Incoming transfers.
         *
         * - amount: exactly two decimals after a dot or comma; anything else
         *   (e.g. "1,234.56") doesn't match and goes to unrecognized messages.
         * - currency: the euro sign (written as \u20AC, which the regex engine
         *   understands) or a written "EUR".
         * - merchant: the sender's name, everything after "od" to the end.
         */
        private val TRANSFER_PATTERN = Regex(
            """^\s*(?<date>\d{1,2}\.\d{1,2}\.\d{4})\s*-\s*""" +
                    """(?<operation>Prejeli\s+ste)\s+""" +
                    """(?<amount>\d+[.,]\d{2})\s*(?<currency>\u20AC|EUR)\s+""" +
                    """od\s+(?<merchant>.+?)\s*$""",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
        )
    }
}