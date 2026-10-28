package com.aljaz.spendingtracker.ui.sharing

import com.aljaz.spendingtracker.domain.sharing.SharedAccount

/**
 * Everything the Shared account screen shows at one moment.
 *
 * @property account the logged-in account, or null when the login form is shown
 * @property serverUrl, email, password the login form fields
 * @property isWorking true while logging in; the button is disabled meanwhile
 * @property errorMessage why the last login failed, or null
 * @property showLogOutConfirm true while the "Log out?" dialog is open
 */
data class SharedAccountUiState(
    val account: SharedAccount? = null,
    val serverUrl: String = SharedAccount.DEFAULT_SERVER_URL,
    val email: String = "",
    val password: String = "",
    val isWorking: Boolean = false,
    val errorMessage: String? = null,
    val showLogOutConfirm: Boolean = false
)