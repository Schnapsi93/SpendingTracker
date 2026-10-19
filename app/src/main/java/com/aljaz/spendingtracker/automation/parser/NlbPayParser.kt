package com.aljaz.spendingtracker.automation.parser

import java.time.ZoneId
import javax.inject.Inject

/**
 * Parser for NLB card transaction notifications from the NLB Pay app.
 *
 * Example:
 * `POS NAKUP 19.10.2026 11:52, kartica ***5802, znesek 10,96 EUR,
 *  CHUTIS LJUBLJANA SI. Info: 0038614772000. NLB`
 *
 * Same building blocks as the DH SMS, but no "Obvestilo o transakciji:"
 * prefix, no comma after the operation, and it ends with ". Info: ...".
 */
class NlbPayParser @Inject constructor() : BankMessageParser {

    override val name: String = "NLB (NLB Pay notification)"

    /** For notifications the sender is the posting app's package name. */
    override fun acceptsSender(sender: String): Boolean =
        sender == PACKAGE_NAME

    override fun parse(text: String, zone: ZoneId): ParsedBankMessage? {
        val groups = PATTERN.find(text.trim())?.groups ?: return null

        val operation = groups["operation"]!!.value.trim().uppercase()
        val type = BankMessageFields.typeOf(operation) ?: return null // unknown: don't guess
        val timestamp = BankMessageFields.parseDate(groups["date"]!!.value, zone) ?: return null
        val amountMinor = BankMessageFields.parseAmountMinor(groups["amount"]!!.value) ?: return null

        return ParsedBankMessage(
            operation = operation,
            type = type,
            amountMinor = amountMinor,
            currency = groups["currency"]!!.value.uppercase(),
            timestamp = timestamp,
            merchant = BankMessageFields.cleanMerchant(groups["merchant"]!!.value),
            cardLast4 = groups["card"]!!.value
        )
    }

    companion object {
        /** Package name of the NLB Pay app (from its Play Store link). */
        const val PACKAGE_NAME = "si.nlbpay.slovenija"

        /**
         * The notification layout, with the same named groups as DhSmsParser.
         *
         * - operation: everything before the date that isn't a digit or comma,
         *   e.g. "POS NAKUP"; typeOf() then decides if it's known.
         * - \s+ and \s* also match line breaks, so a wrapped notification works.
         * - merchant: lazy (.+?) up to ". Info:", so names with dots stay complete.
         */
        private val PATTERN = Regex(
            """^\s*(?<operation>[^,\d]+?)\s+""" +
                    """(?<date>\d{1,2}\.\d{1,2}\.\d{4}\s+\d{1,2}:\d{2}),""" +
                    """\s*kartica\s*\*+(?<card>\d{4}),""" +
                    """\s*znesek\s+(?<amount>[\d.]+,\d{2})\s+(?<currency>[A-Za-z]{3}),""" +
                    """\s*(?<merchant>.+?)\.\s*Info:""",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
        )
    }
}