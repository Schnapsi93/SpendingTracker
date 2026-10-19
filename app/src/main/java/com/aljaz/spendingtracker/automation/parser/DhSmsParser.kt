package com.aljaz.spendingtracker.automation.parser

import java.time.ZoneId
import javax.inject.Inject

/**
 * Parser for Delavska hranilnica (DH d.d.) card transaction SMS messages.
 *
 * Example:
 * `Obvestilo o transakciji: POS NAKUP, 13.10.2026 15:37, kartica ***1754,
 *  znesek 161,77 EUR, BS LOGO BREZOVICA. DH d.d.`
 */
class DhSmsParser @Inject constructor() : BankMessageParser {

    override val name: String = "Delavska hranilnica (SMS)"

    /** Compares sender names ignoring case, spaces and dots: "DH d.d." = "dh dd" = "DHdd". */
    override fun acceptsSender(sender: String): Boolean =
        simplify(sender) == simplify(SENDER)

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

    /** Lowercase letters and digits only, for comparing sender names. */
    private fun simplify(text: String): String =
        text.lowercase().filter { it.isLetterOrDigit() }

    companion object {
        /** The sender name the bank's SMS messages arrive from. */
        const val SENDER = "DH d.d."

        /**
         * The message layout. Named groups (?<name>...) capture each piece:
         * operation, date, card, amount, currency, merchant.
         *
         * The merchant is "everything up to the final '. DH d.d.'", matched
         * lazily (.+?) so names that contain dots themselves, like
         * "OTP BANKA D.D.", stay complete.
         */
        private val PATTERN = Regex(
            """Obvestilo o transakciji:\s*(?<operation>[^,]+),""" +
                    """\s*(?<date>\d{1,2}\.\d{1,2}\.\d{4}\s+\d{1,2}:\d{2}),""" +
                    """\s*kartica\s*\*+(?<card>\d{4}),""" +
                    """\s*znesek\s+(?<amount>[\d.]+,\d{2})\s+(?<currency>[A-Za-z]{3}),""" +
                    """\s*(?<merchant>.+?)\.\s*DH d\.d\.\s*$""",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
        )
    }
}