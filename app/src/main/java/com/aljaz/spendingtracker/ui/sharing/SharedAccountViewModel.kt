package com.aljaz.spendingtracker.ui.sharing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aljaz.spendingtracker.core.AppResult
import com.aljaz.spendingtracker.domain.sharing.SharedAccountRepository
import com.aljaz.spendingtracker.domain.usecase.LogInSharedAccountUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Logic behind the Shared account screen: the login form, the account
 * summary, and logging out.
 */
@HiltViewModel
class SharedAccountViewModel @Inject constructor(
    private val repository: SharedAccountRepository,
    private val logIn: LogInSharedAccountUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SharedAccountUiState())
    val uiState: StateFlow<SharedAccountUiState> = _uiState.asStateFlow()

    init {
        // Switches between the form and the summary on login and logout.
        viewModelScope.launch {
            repository.account.collect { account ->
                _uiState.update { it.copy(account = account) }
            }
        }
    }

    fun onServerUrlChange(text: String) =
        _uiState.update { it.copy(serverUrl = text, errorMessage = null) }

    fun onEmailChange(text: String) =
        _uiState.update { it.copy(email = text, errorMessage = null) }

    fun onPasswordChange(text: String) =
        _uiState.update { it.copy(password = text, errorMessage = null) }

    /** Logs in; on success the screen switches to the account summary by itself. */
    fun onLogInClick() {
        val state = _uiState.value
        if (state.isWorking) return
        _uiState.update { it.copy(isWorking = true, errorMessage = null) }

        viewModelScope.launch {
            when (val result = logIn(state.serverUrl, state.email, state.password)) {
                is AppResult.Success ->
                    // Clear the typed password from memory; it is stored encrypted now.
                    _uiState.update { it.copy(isWorking = false, password = "") }
                is AppResult.Error ->
                    _uiState.update { it.copy(isWorking = false, errorMessage = result.message) }
            }
        }
    }

    /** Asks for confirmation first. */
    fun onLogOutClick() =
        _uiState.update { it.copy(showLogOutConfirm = true) }

    fun onLogOutDismiss() =
        _uiState.update { it.copy(showLogOutConfirm = false) }

    fun onLogOutConfirm() {
        _uiState.update { it.copy(showLogOutConfirm = false) }
        viewModelScope.launch { repository.logOut() }
    }
}