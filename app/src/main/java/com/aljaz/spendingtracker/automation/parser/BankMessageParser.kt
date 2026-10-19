package com.aljaz.spendingtracker.automation.parser

import java.time.ZoneId

/**
 * Understands the transaction messages of one bank (or one message format).
 *
 * Messages can arrive as SMS or as another app's notification; each format
 * gets its own class implementing this interface. Nothing else has to change
 * to support a new one: register it in AutomationModule.
 */
interface BankMessageParser {

    /** Human-readable name, e.g. "Delavska hranilnica (SMS)". */
    val name: String

    /**
     * @param sender the SMS sender name ("DH d.d.") for SMS, or the posting
     *               app's package name ("si.nlbpay.slovenija") for notifications
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