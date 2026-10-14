package com.aljaz.spendingtracker.automation.parser

import com.aljaz.spendingtracker.core.MoneyParser
import com.aljaz.spendingtracker.domain.model.TransactionType
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
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
        val match = PATTERN.find(text.trim()) ?: return null
        val groups = match.groups

        val operation = groups["operation"]!!.value.trim().uppercase()
        val type = typeOf(operation) ?: return null // unknown operation: don't guess

        val timestamp = parseDate(groups["date"]!!.value, zone) ?: return null

        // "1.234,56" -> "1234,56": drop thousands dots, keep the decimal comma.
        val amountMinor = MoneyParser.parseToMinor(groups["amount"]!!.value.replace(".", ""))
            ?: return null

        return ParsedBankMessage(
            operation = operation,
            type = type,
            amountMinor = amountMinor,
            currency = groups["currency"]!!.value.uppercase(),
            timestamp = timestamp,
            merchant = cleanMerchant(groups["merchant"]!!.value),
            cardLast4 = groups["card"]!!.value
        )
    }

    /** Maps the bank's operation label to income or expense; null if unknown. */
    private fun typeOf(operation: String): TransactionType? = when {
        "NAKUP" in operation -> TransactionType.EXPENSE // POS NAKUP, SPLET/TEL NAKUP
        "DVIG" in operation -> TransactionType.EXPENSE  // BA DVIG (ATM withdrawal)
        else -> null
    }

    /** "13.10.2026 15:37" in the given zone, as epoch millis; null if invalid. */
    private fun parseDate(text: String, zone: ZoneId): Long? =
        try {
            LocalDateTime.parse(text.replace(Regex("\\s+"), " "), DATE_FORMAT)
                .atZone(zone)
                .toInstant()
                .toEpochMilli()
        } catch (e: DateTimeParseException) {
            null
        }

    /**
     * Tidies the merchant name: collapses repeated spaces and removes a
     * trailing dash, e.g. "PETROL BS LJUBLJANA -" becomes "PETROL BS LJUBLJANA".
     */
    private fun cleanMerchant(raw: String): String =
        raw.replace(Regex("\\s+"), " ")
            .trim()
            .trimEnd('-', ' ')
            .trim()

    /** Lowercase letters and digits only, for comparing sender names. */
    private fun simplify(text: String): String =
        text.lowercase().filter { it.isLetterOrDigit() }

    companion object {
        /** The sender name the bank's SMS messages arrive from. */
        const val SENDER = "DH d.d."

        /** Accepts "06.10.2026 20:05" as well as "6.10.2026 20:05". */
        private val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("d.M.yyyy H:mm")

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