package com.aljaz.spendingtracker.domain.model

/**
 * Sync status of a transaction with the shared PocketBase server.
 *
 * Only shared (split) transactions ever leave the phone, so most
 * transactions stay LOCAL_ONLY forever.
 */
enum class SyncState {
    /** Personal transaction. Never uploaded. */
    LOCAL_ONLY,

    /** Shared transaction that was created or edited but not uploaded yet. */
    PENDING,

    /** Shared transaction whose latest version is on the server. */
    SYNCED
}