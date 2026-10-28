package com.aljaz.spendingtracker

import android.app.Application
import com.aljaz.spendingtracker.automation.notifications.AndroidTransactionNotifier
import com.aljaz.spendingtracker.data.backup.BackupScheduler
import com.aljaz.spendingtracker.domain.sharing.SharedSyncRepository
import com.aljaz.spendingtracker.domain.usecase.EnsureDefaultCategoriesUseCase
import com.aljaz.spendingtracker.domain.usecase.EnsureStarterRulesUseCase
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Application entry point.
 *
 * The @HiltAndroidApp annotation tells Hilt to generate the dependency
 * container that lives as long as the app process does.
 */
@HiltAndroidApp
class SpendingTrackerApp : Application() {

    /** Filled in by Hilt during super.onCreate(). */
    @Inject
    lateinit var ensureDefaultCategories: EnsureDefaultCategoriesUseCase

    /** Filled in by Hilt during super.onCreate(). */
    @Inject
    lateinit var ensureStarterRules: EnsureStarterRulesUseCase

    /** Filled in by Hilt during super.onCreate(). */
    @Inject
    lateinit var sharedSync: SharedSyncRepository

    /**
     * Scope for one-off startup work that should not block the UI.
     * SupervisorJob keeps one failing task from cancelling the others.
     */
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate() // Hilt injects the fields above here
        appScope.launch {
            ensureDefaultCategories()
            // After the categories, so starter rules reuse "Groceries" instead of creating a copy.
            ensureStarterRules()
        }
        BackupScheduler.schedule(this) // daily check for the monthly cloud backup
        AndroidTransactionNotifier.createChannel(this) // "New transactions" notifications
        sharedSync.startAutoSync(appScope) // shared expenses: hourly, on start and after changes
    }
}