package com.aljaz.spendingtracker.domain.sharing

import com.aljaz.spendingtracker.core.AppResult
import kotlinx.coroutines.flow.StateFlow

/** Logs in to the shared expenses server and remembers the account. */
interface SharedAccountRepository {

    /** The logged-in account, or null when logged out. Updates on login and logout. */
    val account: StateFlow<SharedAccount?>

    /**
     * Logs in, finds the user's household and partner, and remembers everything.
     *
     * @param serverUrl already cleaned up: https, no trailing slash
     * @return the account, or [AppResult.Error] with a message the user can read
     */
    suspend fun logIn(serverUrl: String, email: String, password: String): AppResult<SharedAccount>

    /** Forgets the account, token and password. Local transactions are not touched. */
    suspend fun logOut()
}