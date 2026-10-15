package com.aljaz.spendingtracker.automation

import com.aljaz.spendingtracker.automation.notifications.TransactionNotifier
import com.aljaz.spendingtracker.automation.parser.BankMessageParser
import com.aljaz.spendingtracker.automation.parser.ParsedBankMessage
import com.aljaz.spendingtracker.domain.model.Transaction
import com.aljaz.spendingtracker.domain.model.TransactionSource
import com.aljaz.spendingtracker.domain.model.UnrecognizedMessage
import com.aljaz.spendingtracker.domain.repository.CategoryRepository
import com.aljaz.spendingtracker.domain.repository.CategoryRuleRepository
import com.aljaz.spendingtracker.domain.repository.TransactionRepository
import com.aljaz.spendingtracker.domain.repository.UnrecognizedMessageRepository
import com.aljaz.spendingtracker.domain.rules.CategoryRuleMatcher
import com.aljaz.spendingtracker.domain.rules.RuleText
import javax.inject.Inject

/** What happened to one incoming message. */
sealed interface BankMessageResult {
    /** The sender is not a known bank; the message was ignored. */
    data object NotFromBank : BankMessageResult

    /** From a known bank, but no parser could read it; kept for the user to inspect. */
    data object Unrecognized : BankMessageResult

    /** The same transaction was already saved earlier. */
    data object Duplicate : BankMessageResult

    /** Saved as a new transaction. */
    data class Saved(
        val transaction: Transaction,
        val categoryName: String?
    ) : BankMessageResult
}

/**
 * Turns an incoming bank message into a saved transaction:
 * pick the parser for the sender, parse, categorize with the keyword
 * rules, and save, skipping duplicates. Bank messages that can't be
 * parsed are kept as [UnrecognizedMessage]s. The user is notified about
 * both, unless notifications are turned off.
 *
 * Used by the SMS receiver, the "Test with a message" field and the
 * "Try again" button, so all of them behave exactly the same.
 */
class BankMessageProcessor @Inject constructor(
    // Every registered parser (see AutomationModule).
    private val parsers: Set<@JvmSuppressWildcards BankMessageParser>,
    private val ruleRepository: CategoryRuleRepository,
    private val categoryRepository: CategoryRepository,
    private val transactionRepository: TransactionRepository,
    private val unrecognizedRepository: UnrecognizedMessageRepository,
    private val matcher: CategoryRuleMatcher,
    private val notifier: TransactionNotifier
) {

    /**
     * @param sender the SMS sender name or number, as Android reports it
     * @param body the full message text
     * @param source how the message arrived (SMS, or Wallet later)
     * @param rememberUnrecognized whether to keep the message if it can't be parsed
     * @param notify whether to show a notification about the result
     */
    suspend fun process(
        sender: String,
        body: String,
        source: TransactionSource,
        rememberUnrecognized: Boolean = true,
        notify: Boolean = true
    ): BankMessageResult {
        // Privacy: anything not from a known bank stops here, unread and unstored.
        val parser = parsers.firstOrNull { it.acceptsSender(sender) }
            ?: return BankMessageResult.NotFromBank

        val parsed = parser.parse(body)
        if (parsed == null) {
            if (rememberUnrecognized) {
                unrecognizedRepository.add(
                    UnrecognizedMessage(
                        sender = sender,
                        body = body,
                        receivedAt = System.currentTimeMillis(),
                        source = source
                    )
                )
                if (notify) notifier.notifyUnrecognized()
            }
            return BankMessageResult.Unrecognized
        }

        val rule = matcher.match(ruleRepository.getAll(), parsed.merchant, parsed.operation)

        val transaction = Transaction(
            type = parsed.type,
            amountMinor = parsed.amountMinor,
            currency = parsed.currency,
            categoryId = rule?.categoryId,
            title = parsed.merchant,
            timestamp = parsed.timestamp,
            source = source,
            rawText = body,
            dedupeHash = dedupeHashOf(parsed),
            reviewed = false // shown in the review queue until confirmed
        )

        // The database rejects a second row with the same dedupeHash.
        val id = transactionRepository.add(transaction) ?: return BankMessageResult.Duplicate

        val categoryName = rule?.categoryId?.let { categoryRepository.getById(it)?.name }
        val saved = transaction.copy(id = id)
        if (notify) notifier.notifySaved(saved, categoryName)
        return BankMessageResult.Saved(saved, categoryName)
    }

    /**
     * Runs a stored unrecognized message through the parsers again,
     * e.g. after a parser learned a new format. If it can now be read
     * (or was already saved), it is removed from the unrecognized list.
     * Never notifies: the user is looking at the Review screen already.
     */
    suspend fun retry(message: UnrecognizedMessage): BankMessageResult {
        val result = process(
            sender = message.sender,
            body = message.body,
            source = message.source,
            rememberUnrecognized = false, // it's already stored
            notify = false
        )
        if (result is BankMessageResult.Saved || result == BankMessageResult.Duplicate) {
            unrecognizedRepository.delete(message.id)
        }
        return result
    }

    /**
     * Fingerprint of one purchase. The same SMS delivered twice gives the
     * same fingerprint, so it is saved only once.
     */
    private fun dedupeHashOf(parsed: ParsedBankMessage): String =
        listOf(
            parsed.timestamp,
            parsed.amountMinor,
            parsed.currency,
            RuleText.normalize(parsed.merchant),
            parsed.cardLast4
        ).joinToString("|")
}