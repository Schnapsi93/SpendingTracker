package com.aljaz.spendingtracker.domain.sharing

/**
 * How syncing with the shared server is going, for display.
 *
 * @property isSyncing true while a sync run is in progress
 * @property lastSuccessAt when the last sync finished without errors, epoch millis, or null
 * @property lastError why the most recent sync failed, or null if it worked
 */
data class SyncStatus(
    val isSyncing: Boolean = false,
    val lastSuccessAt: Long? = null,
    val lastError: String? = null
)