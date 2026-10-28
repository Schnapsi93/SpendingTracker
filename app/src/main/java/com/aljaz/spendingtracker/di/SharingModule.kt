package com.aljaz.spendingtracker.di

import com.aljaz.spendingtracker.data.sharing.SharedAccountRepositoryImpl
import com.aljaz.spendingtracker.domain.sharing.SharedAccountRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Connects the shared expenses interfaces to their implementations. */
@Module
@InstallIn(SingletonComponent::class)
abstract class SharingModule {

    @Binds
    @Singleton
    abstract fun bindSharedAccountRepository(
        impl: SharedAccountRepositoryImpl
    ): SharedAccountRepository
}