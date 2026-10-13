package com.aljaz.spendingtracker.di

import android.content.Context
import androidx.room.Room
import com.aljaz.spendingtracker.data.local.AppDatabase
import com.aljaz.spendingtracker.data.local.dao.TransactionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Tells Hilt how to build the database and its DAOs.
 *
 * Installed in [SingletonComponent], so everything provided here
 * is created once and shared across the whole app.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    /** Builds the single [AppDatabase] instance for the whole app. */
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "spending_tracker.db"
        ).build()

    /** Exposes the transaction DAO so it can be injected directly. */
    @Provides
    fun provideTransactionDao(database: AppDatabase): TransactionDao =
        database.transactionDao()
}