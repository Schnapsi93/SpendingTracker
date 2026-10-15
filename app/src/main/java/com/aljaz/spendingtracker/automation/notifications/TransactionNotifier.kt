package com.aljaz.spendingtracker.automation.notifications

import com.aljaz.spendingtracker.domain.model.Transaction

/**
 * Tells the user about automatically added transactions.
 * An interface, so the processing code doesn't depend on Android notifications
 * and could be tested with a fake.
 */
interface TransactionNotifier {

    /** A bank message was saved as [transaction]. */
    fun notifySaved(transaction: Transaction, categoryName: String?)

    /** A bank message arrived but couldn't be read. */
    fun notifyUnrecognized()
}