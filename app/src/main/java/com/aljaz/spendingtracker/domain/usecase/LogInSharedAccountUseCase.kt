package com.aljaz.spendingtracker.domain.usecase

import com.aljaz.spendingtracker.core.AppResult
import com.aljaz.spendingtracker.domain.sharing.SharedAccount
import com.aljaz.spendingtracker.domain.sharing.SharedAccountRepository
import javax.inject.Inject

/**
 * Checks the login form and logs in to the shared expenses server.
 */
class LogInSharedAccountUseCase @Inject constructor(
    private val repository: SharedAccountRepository
) {

    /**
     * @return the logged-in account, or [AppResult.Error] with a message the user can read
     */
    suspend operator fun invoke(
        serverUrl: String,
        email: String,
        password: String
    ): AppResult<SharedAccount> {
        val url = normalizeUrl(serverUrl)
        val cleanEmail = email.trim()

        when {
            url.isEmpty() -> return AppResult.Error("Enter the server address.")
            // Passwords and money data must never travel unencrypted.
            !url.startsWith("https://") ->
                return AppResult.Error("Use a secure address starting with https://")
            '@' !in cleanEmail -> return AppResult.Error("Enter your email address.")
            password.isEmpty() -> return AppResult.Error("Enter your password.")
        }

        return repository.logIn(url, cleanEmail, password)
    }

    /** "spender.aljaz.top/" becomes "https://spender.aljaz.top". */
    private fun normalizeUrl(raw: String): String {
        val trimmed = raw.trim().trimEnd('/')
        if (trimmed.isEmpty()) return ""
        return if ("://" in trimmed) trimmed else "https://$trimmed"
    }
}