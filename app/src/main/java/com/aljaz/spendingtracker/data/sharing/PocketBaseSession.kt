package com.aljaz.spendingtracker.data.sharing

import com.aljaz.spendingtracker.core.AppResult
import com.aljaz.spendingtracker.domain.sharing.SharedAccount
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Gets a fresh login token for the saved account.
 *
 * Logs in again with the saved password every time it is asked. That costs
 * one small request, but an expired token can never get in the way.
 * Used by the sync and by settle-up.
 */
@Singleton
class PocketBaseSession @Inject constructor(
    private val client: PocketBaseClient,
    private val accountStore: SharedAccountStore
) {

    /** @return a fresh token, or [AppResult.Error] with a readable message */
    suspend fun freshToken(account: SharedAccount): AppResult<String> {
        val password = accountStore.password()
            ?: return AppResult.Error(
                "The saved login can't be read. Log out and in again under Shared account."
            )
        return when (val result = client.authWithPassword(account.serverUrl, account.email, password)) {
            is AppResult.Error -> result
            is AppResult.Success -> {
                accountStore.updateToken(result.data.token)
                AppResult.Success(result.data.token)
            }
        }
    }
}