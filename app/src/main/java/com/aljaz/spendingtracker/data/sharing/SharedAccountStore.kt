package com.aljaz.spendingtracker.data.sharing

import android.content.Context
import com.aljaz.spendingtracker.data.backup.SecretCipher
import com.aljaz.spendingtracker.domain.sharing.SharedAccount
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Keeps the shared account in private app preferences.
 *
 * The login token and the password are stored only in encrypted form
 * (see [SecretCipher]). The password is kept so the sync can log in again
 * by itself (PocketBase tokens expire after 7 days).
 */
@Singleton
class SharedAccountStore @Inject constructor(
    @ApplicationContext context: Context,
    private val cipher: SecretCipher
) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // Read once at startup (a few small values), then kept up to date in memory.
    private val _account = MutableStateFlow(readAccount())

    /** The saved account, or null when logged out. */
    val account: StateFlow<SharedAccount?> = _account.asStateFlow()

    /** Saves the account and its secrets after a successful login. */
    fun save(account: SharedAccount, token: String, password: String) {
        prefs.edit().apply {
            putString(KEY_URL, account.serverUrl)
            putString(KEY_USER_ID, account.userId)
            putString(KEY_EMAIL, account.email)
            putString(KEY_DISPLAY_NAME, account.displayName)
            putString(KEY_HOUSEHOLD_ID, account.householdId)
            putString(KEY_HOUSEHOLD_NAME, account.householdName)
            putString(KEY_PARTNER_ID, account.partnerId)
            putString(KEY_PARTNER_NAME, account.partnerName)
            putString(KEY_TOKEN, cipher.encrypt(token))
            putString(KEY_PASSWORD, cipher.encrypt(password))
        }.apply()
        _account.value = account
    }

    /** Replaces the token after the sync logged in again. */
    fun updateToken(token: String) {
        prefs.edit().putString(KEY_TOKEN, cipher.encrypt(token)).apply()
    }

    /** The current login token, or null if missing or it can't be decrypted. */
    fun token(): String? =
        prefs.getString(KEY_TOKEN, null)?.let { cipher.decrypt(it) }

    /** The saved password, or null if missing or it can't be decrypted. */
    fun password(): String? =
        prefs.getString(KEY_PASSWORD, null)?.let { cipher.decrypt(it) }

    /** Forgets everything. */
    fun clear() {
        prefs.edit().clear().apply()
        _account.value = null
    }

    /** Rebuilds the account from preferences; null if nobody is logged in. */
    private fun readAccount(): SharedAccount? {
        val userId = prefs.getString(KEY_USER_ID, null) ?: return null
        val householdId = prefs.getString(KEY_HOUSEHOLD_ID, null) ?: return null
        return SharedAccount(
            serverUrl = prefs.getString(KEY_URL, null).orEmpty(),
            userId = userId,
            email = prefs.getString(KEY_EMAIL, null).orEmpty(),
            displayName = prefs.getString(KEY_DISPLAY_NAME, null).orEmpty(),
            householdId = householdId,
            householdName = prefs.getString(KEY_HOUSEHOLD_NAME, null).orEmpty(),
            partnerId = prefs.getString(KEY_PARTNER_ID, null),
            partnerName = prefs.getString(KEY_PARTNER_NAME, null)
        )
    }

    private companion object {
        const val PREFS_NAME = "shared_account"
        const val KEY_URL = "server_url"
        const val KEY_USER_ID = "user_id"
        const val KEY_EMAIL = "email"
        const val KEY_DISPLAY_NAME = "display_name"
        const val KEY_HOUSEHOLD_ID = "household_id"
        const val KEY_HOUSEHOLD_NAME = "household_name"
        const val KEY_PARTNER_ID = "partner_id"
        const val KEY_PARTNER_NAME = "partner_name"
        const val KEY_TOKEN = "token_encrypted"
        const val KEY_PASSWORD = "password_encrypted"
    }
}