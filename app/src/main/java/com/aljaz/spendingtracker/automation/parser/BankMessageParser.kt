package com.aljaz.spendingtracker.automation.parser

import java.time.ZoneId

/**
 * Understands the transaction messages of one bank (or one message format).
 *
 * Supporting another bank or Google Wallet later means writing one more
 * class that implements this interface; nothing else has to change.
 */
interface BankMessageParser {

    /** Human-readable name, e.g. "Delavska hranilnica (SMS)". */
    val name: String

    /**
     * @param sender the SMS sender or notification source, as reported by Android
     * @return true if messages from this sender belong to this parser
     */
    fun acceptsSender(sender: String): Boolean

    /**
     * @param text the full message text
     * @param zone time zone of the dates written in the message
     * @return the extracted data, or null if this isn't a transaction
     *         message this parser understands (e.g. a verification code)
     */

    fun parse(text: String, zone: ZoneId = ZoneId.systemDefault()): ParsedBankMessage?
}