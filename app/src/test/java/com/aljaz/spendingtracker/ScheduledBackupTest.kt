package com.aljaz.spendingtracker

import com.aljaz.spendingtracker.core.AppResult
import com.aljaz.spendingtracker.domain.backup.BackupSettings
import com.aljaz.spendingtracker.domain.backup.BackupSettingsRepository
import com.aljaz.spendingtracker.domain.backup.CloudCredentials
import com.aljaz.spendingtracker.domain.backup.CloudStorage
import com.aljaz.spendingtracker.domain.export.HistoryExporter
import com.aljaz.spendingtracker.domain.usecase.RunCloudBackupUseCase
import com.aljaz.spendingtracker.domain.usecase.RunScheduledBackupUseCase
import com.aljaz.spendingtracker.domain.usecase.ScheduledBackupResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.OutputStream
import java.time.YearMonth
import java.time.ZoneOffset

/** Checks when the monthly backup runs and when it is skipped. */
class ScheduledBackupTest {

    private val utc = ZoneOffset.UTC
    private val credentials =
        CloudCredentials("https://cloud.example.com", "user", "secret", "SpendingTracker")

    /** Settings stored in memory instead of on the phone. */
    private class FakeSettings(
        var credentials: CloudCredentials?,
        var lastBackup: Long?
    ) : BackupSettingsRepository {
        override suspend fun load() = BackupSettings()
        override suspend fun save(settings: BackupSettings, newPassword: String?) = Unit
        override suspend fun loadCredentials() = credentials
        override suspend fun lastBackupAt() = lastBackup
        override suspend fun setLastBackupAt(epochMillis: Long) {
            lastBackup = epochMillis
        }
    }

    /** Pretends to export [count] transactions. */
    private class FakeExporter(private val count: Int) : HistoryExporter {
        override suspend fun export(output: OutputStream): Int {
            output.write("data".toByteArray())
            return count
        }
    }

    /** Counts uploads instead of contacting a server. */
    private class FakeStorage : CloudStorage {
        var uploads = 0
        override suspend fun testConnection(credentials: CloudCredentials) = AppResult.Success(Unit)
        override suspend fun upload(
            credentials: CloudCredentials,
            fileName: String,
            content: ByteArray
        ): AppResult<Unit> {
            uploads++
            return AppResult.Success(Unit)
        }
    }

    /** Epoch millis on the 15th of the given month. */
    private fun midMonth(month: YearMonth): Long =
        month.atDay(15).atStartOfDay(utc).toInstant().toEpochMilli()

    private fun scheduledBackup(
        settings: FakeSettings,
        exporter: FakeExporter,
        storage: FakeStorage
    ) = RunScheduledBackupUseCase(settings, RunCloudBackupUseCase(settings, exporter, storage))

    @Test
    fun doesNothingWhenNotConfigured() = runBlocking {
        val storage = FakeStorage()
        val result = scheduledBackup(FakeSettings(null, null), FakeExporter(5), storage)(utc)
        assertEquals(ScheduledBackupResult.NotConfigured, result)
        assertEquals(0, storage.uploads)
    }

    @Test
    fun skipsWhenAlreadyBackedUpThisMonth() = runBlocking {
        val storage = FakeStorage()
        val settings = FakeSettings(credentials, lastBackup = System.currentTimeMillis())
        val result = scheduledBackup(settings, FakeExporter(5), storage)(utc)
        assertEquals(ScheduledBackupResult.AlreadyDoneThisMonth, result)
        assertEquals(0, storage.uploads)
    }

    @Test
    fun backsUpWhenLastBackupWasLastMonth() = runBlocking {
        val storage = FakeStorage()
        val lastMonth = midMonth(YearMonth.now(utc).minusMonths(1))
        val settings = FakeSettings(credentials, lastBackup = lastMonth)

        val result = scheduledBackup(settings, FakeExporter(5), storage)(utc)

        assertEquals(ScheduledBackupResult.Completed(5), result)
        assertEquals(1, storage.uploads)
        assertTrue(settings.lastBackup!! > lastMonth) // the new time was recorded
    }

    @Test
    fun backsUpWhenNeverBackedUpBefore() = runBlocking {
        val storage = FakeStorage()
        val result = scheduledBackup(FakeSettings(credentials, null), FakeExporter(3), storage)(utc)
        assertEquals(ScheduledBackupResult.Completed(3), result)
        assertEquals(1, storage.uploads)
    }

    @Test
    fun neverUploadsAnEmptyHistory() = runBlocking {
        val storage = FakeStorage()
        val result = scheduledBackup(FakeSettings(credentials, null), FakeExporter(0), storage)(utc)
        assertTrue(result is ScheduledBackupResult.Failed)
        assertEquals(0, storage.uploads)
    }
}