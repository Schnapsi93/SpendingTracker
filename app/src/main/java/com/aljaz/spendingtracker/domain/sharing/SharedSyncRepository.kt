package com.aljaz.spendingtracker.domain.sharing

import com.aljaz.spendingtracker.core.AppResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow

/**
 * Keeps shared expenses in sync between this phone and the server:
 * uploads this user's split expenses (and deletions), downloads the
 * whole household's shared expenses into a local mirror.
 */
interface SharedSyncRepository {

    /** Progress and result of syncing, for display. */
    val status: StateFlow<SyncStatus>

    /**
     * Asks for a sync soon, in the background, as soon as the phone is online.
     * Does nothing when logged out.
     */
    fun requestSync()

    /** Runs a full sync right now and waits for it. Does nothing when logged out. */
    suspend fun syncNow(): AppResult<Unit>

    /** Remembers that a possibly uploaded transaction was deleted on this phone. */
    suspend fun rememberDeletion(uuid: String)

    /**
     * Starts the automatic syncing: hourly, on app start, and shortly after
     * every change that needs uploading. Called once from the Application.
     */
    fun startAutoSync(scope: CoroutineScope)

    /** Forgets downloaded data so the next sync downloads everything again (on login). */
    suspend fun resetDownloads()

    /** Forgets all shared data and stops syncing (on logout). Own transactions stay. */
    suspend fun clearLocalData()
}