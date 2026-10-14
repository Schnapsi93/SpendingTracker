package com.aljaz.spendingtracker.di

import com.aljaz.spendingtracker.automation.parser.BankMessageParser
import com.aljaz.spendingtracker.automation.parser.DhSmsParser
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

/**
 * Registers the bank message parsers.
 *
 * Each @IntoSet line adds one parser to the Set that BankMessageProcessor
 * receives. To support another bank, write its parser and add one
 * more function here.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class AutomationModule {

    @Binds
    @IntoSet
    abstract fun bindDhSmsParser(impl: DhSmsParser): BankMessageParser
}