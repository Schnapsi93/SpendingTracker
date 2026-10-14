package com.aljaz.spendingtracker.di

import com.aljaz.spendingtracker.data.backup.BackupSettingsStore
import com.aljaz.spendingtracker.data.backup.NextcloudClient
import com.aljaz.spendingtracker.domain.backup.BackupSettingsRepository
import com.aljaz.spendingtracker.domain.backup.CloudStorage
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Connects the backup interfaces to their implementations. */
@Module
@InstallIn(SingletonComponent::class)
abstract class BackupModule {

    @Binds
    @Singleton
    abstract fun bindBackupSettingsRepository(
        impl: BackupSettingsStore
    ): BackupSettingsRepository

    @Binds
    @Singleton
    abstract fun bindCloudStorage(
        impl: NextcloudClient
    ): CloudStorage
}