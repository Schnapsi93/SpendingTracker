package com.aljaz.spendingtracker.data.sharing

import com.aljaz.spendingtracker.core.AppResult
import com.aljaz.spendingtracker.domain.sharing.SharedAccount
import com.aljaz.spendingtracker.domain.sharing.SharedAccountRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [SharedAccountRepository] backed by PocketBase ([PocketBaseClient])
 * and private preferences ([SharedAccountStore]).
 */
@Singleton
class SharedAccountRepositoryImpl @Inject constructor(
    private val client: PocketBaseClient,
    private val store: SharedAccountStore
) : SharedAccountRepository {

    override val account: StateFlow<SharedAccount?> = store.account

    override suspend fun logIn(
        serverUrl: String,
        email: String,
        password: String
    ): AppResult<SharedAccount> {
        // 1. Log in.
        val auth = when (val result = client.authWithPassword(serverUrl, email, password)) {
            is AppResult.Error -> return result
            is AppResult.Success -> result.data
        }

        // 2. Find the household. For now one per user; the oldest one is used.
        val households = when (val result = client.fetchHouseholds(serverUrl, auth.token)) {
            is AppResult.Error -> return result
            is AppResult.Success -> result.data
        }
        val household = households.firstOrNull()
            ?: return AppResult.Error(
                "Your account isn't in a household yet. " +
                        "Add it in the PocketBase admin (households → members)."
            )

        // 3. The partner is the other member, if there already is one.
        val partner = household.members.firstOrNull { it.id != auth.userId }

        val account = SharedAccount(
            serverUrl = serverUrl,
            userId = auth.userId,
            email = auth.email,
            displayName = auth.displayName,
            householdId = household.id,
            householdName = household.name,
            partnerId = partner?.id,
            partnerName = partner?.displayName
        )

        // 4. Remember it (encryption and file access happen off the main thread).
        withContext(Dispatchers.IO) { store.save(account, auth.token, password) }
        return AppResult.Success(account)
    }

    override suspend fun logOut() = withContext(Dispatchers.IO) {
        store.clear()
    }
}