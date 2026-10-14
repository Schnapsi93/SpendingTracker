package com.aljaz.spendingtracker

import android.app.Application
import com.aljaz.spendingtracker.domain.usecase.EnsureDefaultCategoriesUseCase
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.aljaz.spendingtracker.data.backup.BackupScheduler

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

    /**
     * Scope for one-off startup work that should not block the UI.
     * SupervisorJob keeps one failing task from cancelling the others.
     */
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate() // Hilt injects the fields above here
        appScope.launch { ensureDefaultCategories() }
        BackupScheduler.schedule(this) // daily check for the monthly cloud backup
    }
}