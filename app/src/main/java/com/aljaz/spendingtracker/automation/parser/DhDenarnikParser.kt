package com.aljaz.spendingtracker.automation.parser

import java.time.ZoneId
import javax.inject.Inject

/**
 * Parser for notifications from the DH Denarnik app (Delavska hranilnica).
 *
 * DH card payments already arrive as SMS (see [DhSmsParser]); this app's
 * notifications add incoming transfers, in the shared format:
 * `23.10.2026 - Prejeli ste 5.00 € od Sabina P.`
 *
 * Any other DH Denarnik notification that looks like it comes from the bank
 * ends up in the unrecognized messages, where a new format can be spotted.
 */
class DhDenarnikParser @Inject constructor() : BankMessageParser {

    override val name: String = "Delavska hranilnica (DH Denarnik notification)"

    /** For notifications the sender is the posting app's package name. */
    override fun acceptsSender(sender: String): Boolean =
        sender == PACKAGE_NAME

    override fun parse(text: String, zone: ZoneId): ParsedBankMessage? =
        IncomingTransferFormat.parse(text, zone)

    companion object {
        /** Package name of the DH Denarnik app (from its Play Store link). */
        const val PACKAGE_NAME = "si.delavskahranilnica.dhdenarnik"
    }
}