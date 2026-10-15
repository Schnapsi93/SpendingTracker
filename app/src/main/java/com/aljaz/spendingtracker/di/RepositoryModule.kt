package com.aljaz.spendingtracker.di

import com.aljaz.spendingtracker.data.repository.TransactionRepositoryImpl
import com.aljaz.spendingtracker.domain.repository.TransactionRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import com.aljaz.spendingtracker.data.repository.CategoryRepositoryImpl
import com.aljaz.spendingtracker.domain.repository.CategoryRepository
import com.aljaz.spendingtracker.data.csv.CsvHistoryExporter
import com.aljaz.spendingtracker.domain.export.HistoryExporter
import com.aljaz.spendingtracker.data.csv.CsvHistoryImporter
import com.aljaz.spendingtracker.domain.export.HistoryImporter
import com.aljaz.spendingtracker.data.repository.CategoryRuleRepositoryImpl
import com.aljaz.spendingtracker.domain.repository.CategoryRuleRepository
import com.aljaz.spendingtracker.data.repository.UnrecognizedMessageRepositoryImpl
import com.aljaz.spendingtracker.domain.repository.UnrecognizedMessageRepository
import com.aljaz.spendingtracker.data.settings.PeriodSettingsStore
import com.aljaz.spendingtracker.domain.settings.PeriodSettingsRepository

/**
 * Connects repository interfaces to their implementations.
 *
 * Each new repository gets one more @Binds line here.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    /** When code asks for [TransactionRepository], provide the Room version. */
    @Binds
    @Singleton
    abstract fun bindTransactionRepository(
        impl: TransactionRepositoryImpl
    ): TransactionRepository


    /** When code asks for [CategoryRepository], provide the Room version. */
    @Binds
    @Singleton
    abstract fun bindCategoryRepository(
        impl: CategoryRepositoryImpl
    ): CategoryRepository


    /** When code asks for a [HistoryExporter], provide the CSV version. */
    @Binds
    @Singleton
    abstract fun bindHistoryExporter(
        impl: CsvHistoryExporter
    ): HistoryExporter


    /** When code asks for a [HistoryImporter], provide the CSV version. */
    @Binds
    @Singleton
    abstract fun bindHistoryImporter(
        impl: CsvHistoryImporter
    ): HistoryImporter


    /** When code asks for [CategoryRuleRepository], provide the Room version. */
    @Binds
    @Singleton
    abstract fun bindCategoryRuleRepository(
        impl: CategoryRuleRepositoryImpl
    ): CategoryRuleRepository


    /** When code asks for [UnrecognizedMessageRepository], provide the Room version. */
    @Binds
    @Singleton
    abstract fun bindUnrecognizedMessageRepository(
        impl: UnrecognizedMessageRepositoryImpl
    ): UnrecognizedMessageRepository


    /** Period settings (month start day), kept in preferences. */
    @Binds
    @Singleton
    abstract fun bindPeriodSettingsRepository(
        impl: PeriodSettingsStore
    ): PeriodSettingsRepository
}