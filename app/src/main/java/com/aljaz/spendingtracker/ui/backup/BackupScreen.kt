package com.aljaz.spendingtracker.ui.backup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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

/**
 * Backup account screen: the Nextcloud settings form, "Save and test",
 * "Back up now", and when the last backup ran. Opened from Settings.
 *
 * @param onBackClick called when the user wants to return to Settings
 * @param viewModel provided by Hilt automatically
 */
@Composable
fun BackupScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BackupViewModel = hiltViewModel()
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
                Text(text = "Backup account", style = MaterialTheme.typography.titleLarge)
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(text = "Nextcloud", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "Backs up your transactions, categories and keyword rules once a " +
                                "month as spending_backup.csv, whenever the phone is online. " +
                                "Use an app password (Nextcloud → Personal settings → Security), " +
                                "not your main password.",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    OutlinedTextField(
                        value = state.serverUrl,
                        onValueChange = viewModel::onUrlChange,
                        label = { Text("Server address") },
                        placeholder = { Text("https://cloud.example.com") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = state.username,
                        onValueChange = viewModel::onUsernameChange,
                        label = { Text("Username") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = state.password,
                        onValueChange = viewModel::onPasswordChange,
                        label = { Text("App password") },
                        placeholder = {
                            Text(if (state.hasSavedPassword) "Saved. Leave empty to keep it" else "")
                        },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = state.folder,
                        onValueChange = viewModel::onFolderChange,
                        label = { Text("Folder in Nextcloud") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(onClick = viewModel::onSaveAndTestClick, enabled = !state.isBusy) {
                        Text(if (state.isWorking) "Connecting…" else "Save and test")
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    Text(
                        text = "Last backup: ${state.lastBackupText ?: "never"}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    // Only possible once a password has been saved.
                    OutlinedButton(
                        onClick = viewModel::onBackupNowClick,
                        enabled = !state.isBusy && state.hasSavedPassword
                    ) {
                        Text(if (state.isBackingUp) "Backing up…" else "Back up now")
                    }

                    state.message?.let { message ->
                        Text(text = message, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}