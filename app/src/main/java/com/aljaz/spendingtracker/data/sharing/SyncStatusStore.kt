package com.aljaz.spendingtracker.data.sharing

import android.content.Context
import com.aljaz.spendingtracker.domain.sharing.SyncStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Remembers how far the download got (the "cursor") and the result of the
 * last sync, in private app preferences.
 */
@Singleton
class SyncStatusStore @Inject constructor(
    @ApplicationContext context: Context
) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _status = MutableStateFlow(
        SyncStatus(
            lastSuccessAt = if (prefs.contains(KEY_LAST_SUCCESS)) {
                prefs.getLong(KEY_LAST_SUCCESS, 0L)
            } else {
                null
            },
            lastError = prefs.getString(KEY_LAST_ERROR, null)
        )
    )
    val status: StateFlow<SyncStatus> = _status.asStateFlow()

    /** The server "updated" time of the newest downloaded change, or null for "nothing yet". */
    fun cursor(): String? = prefs.getString(KEY_CURSOR, null)

    fun saveCursor(cursor: String) {
        prefs.edit().putString(KEY_CURSOR, cursor).apply()
    }

    /** Makes the next sync download everything again. */
    fun resetCursor() {
        prefs.edit().remove(KEY_CURSOR).apply()
    }

    fun setSyncing(isSyncing: Boolean) =
        _status.update { it.copy(isSyncing = isSyncing) }

    fun recordSuccess(epochMillis: Long) {
        prefs.edit()
            .putLong(KEY_LAST_SUCCESS, epochMillis)
            .remove(KEY_LAST_ERROR)
            .apply()
        _status.update { it.copy(lastSuccessAt = epochMillis, lastError = null) }
    }

    fun recordError(message: String) {
        prefs.edit().putString(KEY_LAST_ERROR, message).apply()
        _status.update { it.copy(lastError = message) }
    }

    /** Forgets everything (on logout). */
    fun clear() {
        prefs.edit().clear().apply()
        _status.value = SyncStatus()
    }

    private companion object {
        const val PREFS_NAME = "shared_sync"
        const val KEY_CURSOR = "cursor"
        const val KEY_LAST_SUCCESS = "last_success_at"
        const val KEY_LAST_ERROR = "last_error"
    }
}