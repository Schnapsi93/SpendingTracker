package com.aljaz.spendingtracker.ui.sharing

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aljaz.spendingtracker.domain.sharing.SharedAccount
import com.aljaz.spendingtracker.domain.sharing.SyncStatus
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * Shared account screen: a login form when logged out, and a summary
 * (user, household, partner, sync status) with sync and log-out buttons
 * when logged in.
 *
 * @param onBackClick called when the user wants to return to Settings
 * @param viewModel provided by Hilt automatically
 */
@Composable
fun SharedAccountScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SharedAccountViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(modifier = modifier) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onBackClick) { Text("< Back") }
                Text(text = "Shared account", style = MaterialTheme.typography.titleLarge)
            }

            val account = state.account
            if (account == null) {
                LogInCard(
                    state = state,
                    onServerUrlChange = viewModel::onServerUrlChange,
                    onEmailChange = viewModel::onEmailChange,
                    onPasswordChange = viewModel::onPasswordChange,
                    onLogInClick = viewModel::onLogInClick
                )
            } else {
                AccountCard(
                    account = account,
                    syncStatus = state.syncStatus,
                    onSyncNowClick = viewModel::onSyncNowClick,
                    onLogOutClick = viewModel::onLogOutClick
                )
            }
        }
    }

    if (state.showLogOutConfirm) {
        AlertDialog(
            onDismissRequest = viewModel::onLogOutDismiss,
            title = { Text("Log out of the shared account?") },
            text = {
                Text(
                    "Your own transactions stay on this phone. Shared expenses downloaded " +
                            "from the server are removed here, and come back when you log in again."
                )
            },
            confirmButton = {
                TextButton(onClick = viewModel::onLogOutConfirm) { Text("Log out") }
            },
            dismissButton = {
                TextButton(onClick = viewModel::onLogOutDismiss) { Text("Cancel") }
            }
        )
    }
}

/** The login form: server address, email, password. */
@Composable
private fun LogInCard(
    state: SharedAccountUiState,
    onServerUrlChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onLogInClick: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(text = "Log in", style = MaterialTheme.typography.titleMedium)
            Text(
                text = "Use the account created for you on the shared server. " +
                        "Only expenses in split categories are ever uploaded.",
                style = MaterialTheme.typography.bodyMedium
            )

            OutlinedTextField(
                value = state.serverUrl,
                onValueChange = onServerUrlChange,
                label = { Text("Server address") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = state.email,
                onValueChange = onEmailChange,
                label = { Text("Email") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = state.password,
                onValueChange = onPasswordChange,
                label = { Text("Password") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth()
            )

            Button(onClick = onLogInClick, enabled = !state.isWorking) {
                Text(if (state.isWorking) "Logging in…" else "Log in")
            }

            state.errorMessage?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

/** Who is logged in, which household, the partner, and how syncing goes. */
@Composable
private fun AccountCard(
    account: SharedAccount,
    syncStatus: SyncStatus,
    onSyncNowClick: () -> Unit,
    onLogOutClick: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Logged in as ${account.displayName}",
                style = MaterialTheme.typography.titleMedium
            )
            Text(text = account.email, style = MaterialTheme.typography.bodySmall)

            Text(
                text = "Household: ${account.householdName.ifBlank { "(no name)" }}",
                style = MaterialTheme.typography.bodyMedium
            )
            if (account.hasPartner) {
                Text(
                    text = "Partner: ${account.partnerName}",
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                Text(
                    text = "No partner in this household yet. Add them in the PocketBase " +
                            "admin, then log out and in again.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }
            Text(text = "Server: ${account.serverUrl}", style = MaterialTheme.typography.bodySmall)

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            SyncStatusText(syncStatus)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onSyncNowClick, enabled = !syncStatus.isSyncing) {
                    Text(if (syncStatus.isSyncing) "Syncing…" else "Sync now")
                }
                OutlinedButton(onClick = onLogOutClick) { Text("Log out") }
            }
        }
    }
}

/** "Last synced: …", and the last error in red if the latest attempt failed. */
@Composable
private fun SyncStatusText(status: SyncStatus) {
    val lastSuccess = status.lastSuccessAt?.let { millis ->
        Instant.ofEpochMilli(millis)
            .atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT))
    }

    Text(
        text = when {
            status.isSyncing -> "Syncing…"
            lastSuccess != null -> "Last synced: $lastSuccess"
            else -> "Not synced yet"
        },
        style = MaterialTheme.typography.bodyMedium
    )
    status.lastError?.let { error ->
        Text(
            text = "Last attempt failed: $error",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error
        )
    }
}