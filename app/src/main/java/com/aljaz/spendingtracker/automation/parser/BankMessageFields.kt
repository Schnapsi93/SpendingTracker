package com.aljaz.spendingtracker.automation.parser

import com.aljaz.spendingtracker.core.MoneyParser
import com.aljaz.spendingtracker.domain.model.TransactionType
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/**
 * Converts the text pieces a parser's regex extracted (operation, date,
 * amount, merchant) into values.
 *
 * Shared by all parsers because Slovenian banks write these the same way.
 * Each function returns null instead of throwing when the text is invalid,
 * so a parser can simply give up on a message it doesn't understand.
 */
object BankMessageFields {

    /** Accepts "06.10.2026 20:05" as well as "6.10.2026 20:05". */
    private val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("d.M.yyyy H:mm")

    /**
     * Maps the bank's operation label to income or expense; null if unknown.
     * Unknown operations are not guessed, so the message ends up in the
     * unrecognized list where the user can see it.
     */
    fun typeOf(operation: String): TransactionType? = when {
        "NAKUP" in operation -> TransactionType.EXPENSE // POS NAKUP, SPLET/TEL NAKUP
        "DVIG" in operation -> TransactionType.EXPENSE  // BA DVIG (ATM withdrawal)
        else -> null
    }

    /** "13.10.2026 15:37" in the given zone, as epoch millis; null if invalid. */
    fun parseDate(text: String, zone: ZoneId): Long? =
        try {
            LocalDateTime.parse(text.replace(Regex("\\s+"), " "), DATE_FORMAT)
                .atZone(zone)
                .toInstant()
                .toEpochMilli()
        } catch (e: DateTimeParseException) {
            null
        }

    /**
     * Slovenian amount to minor units: "1.234,56" -> 123456.
     * Drops the thousands dots, then MoneyParser handles the decimal comma.
     */
    fun parseAmountMinor(text: String): Long? =
        MoneyParser.parseToMinor(text.replace(".", ""))

    /**
     * Tidies the merchant name: collapses repeated spaces and line breaks,
     * and removes a trailing dash, e.g. "PETROL BS LJUBLJANA -" becomes
     * "PETROL BS LJUBLJANA".
     */
    fun cleanMerchant(raw: String): String =
        raw.replace(Regex("\\s+"), " ")
            .trim()
            .trimEnd('-', ' ')
            .trim()
}