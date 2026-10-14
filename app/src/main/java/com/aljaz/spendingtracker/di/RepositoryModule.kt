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
}