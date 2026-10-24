package com.aljaz.spendingtracker.di

import com.aljaz.spendingtracker.automation.notifications.AndroidTransactionNotifier
import com.aljaz.spendingtracker.automation.notifications.TransactionNotifier
import com.aljaz.spendingtracker.automation.parser.BankMessageParser
import com.aljaz.spendingtracker.automation.parser.DhDenarnikParser
import com.aljaz.spendingtracker.automation.parser.DhSmsParser
import com.aljaz.spendingtracker.automation.parser.NlbPayParser
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import javax.inject.Singleton

/**
 * Registers the bank message parsers and the transaction notifier.
 *
 * Each @IntoSet function adds one parser to the Set that BankMessageProcessor
 * receives (and that the notification listener uses as its whitelist).
 * To support another bank or app, write its parser and add one more
 * function here.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class AutomationModule {

    /** Delavska hranilnica, via SMS (card payments). */
    @Binds
    @IntoSet
    abstract fun bindDhSmsParser(impl: DhSmsParser): BankMessageParser

    /** Delavska hranilnica, via DH Denarnik app notifications (incoming transfers). */
    @Binds
    @IntoSet
    abstract fun bindDhDenarnikParser(impl: DhDenarnikParser): BankMessageParser

    /** NLB, via NLB Pay app notifications (card payments, incoming transfers). */
    @Binds
    @IntoSet
    abstract fun bindNlbPayParser(impl: NlbPayParser): BankMessageParser

    /** Notifications about automatically added transactions. */
    @Binds
    @Singleton
    abstract fun bindTransactionNotifier(impl: AndroidTransactionNotifier): TransactionNotifier
}