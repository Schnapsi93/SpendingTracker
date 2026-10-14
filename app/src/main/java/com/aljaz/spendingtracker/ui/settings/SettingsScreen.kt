package com.aljaz.spendingtracker.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
 * Settings screen: CSV export and import, and the Nextcloud backup.
 *
 * @param onBackClick called when the user wants to return to Home
 * @param viewModel provided by Hilt automatically
 */
@Composable
fun SettingsScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    // Opens Android's "Save as" picker; the chosen location goes to the ViewModel.
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv"),
        onResult = viewModel::onExportDestinationChosen
    )

    // Opens Android's file picker for choosing a file to import.
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = viewModel::onImportFileChosen
    )

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
                Text(text = "Settings", style = MaterialTheme.typography.titleLarge)
            }

            // ----- Export & import -----
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(text = "Export & import", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "Export saves your complete history as a CSV file " +
                                "(semicolon-separated, opens in Excel and LibreOffice). " +
                                "Import adds transactions from such a file; rows already " +
                                "in the app are skipped.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { exportLauncher.launch(viewModel.suggestedFileName()) },
                            enabled = !state.isBusy
                        ) {
                            Text(if (state.isExporting) "Exporting…" else "Export to CSV")
                        }
                        OutlinedButton(
                            // "*/*" allows any file: phones label CSV files
                            // inconsistently, and the importer checks the content anyway.
                            onClick = { importLauncher.launch(arrayOf("*/*")) },
                            enabled = !state.isBusy
                        ) {
                            Text(if (state.isImporting) "Importing…" else "Import CSV")
                        }
                    }
                    state.message?.let { message ->
                        Text(text = message, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            // ----- Nextcloud backup -----
            CloudBackupCard(
                form = state.cloud,
                onUrlChange = viewModel::onCloudUrlChange,
                onUsernameChange = viewModel::onCloudUsernameChange,
                onPasswordChange = viewModel::onCloudPasswordChange,
                onFolderChange = viewModel::onCloudFolderChange,
                onSaveAndTest = viewModel::onSaveAndTestClick
            )
        }
    }
}

/** The Nextcloud settings form with its "Save and test connection" button. */
@Composable
private fun CloudBackupCard(
    form: CloudFormState,
    onUrlChange: (String) -> Unit,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onFolderChange: (String) -> Unit,
    onSaveAndTest: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(text = "Nextcloud backup", style = MaterialTheme.typography.titleMedium)
            Text(
                text = "Use an app password (Nextcloud → Personal settings → Security), " +
                        "not your main password.",
                style = MaterialTheme.typography.bodyMedium
            )

            OutlinedTextField(
                value = form.serverUrl,
                onValueChange = onUrlChange,
                label = { Text("Server address") },
                placeholder = { Text("https://cloud.example.com") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = form.username,
                onValueChange = onUsernameChange,
                label = { Text("Username") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = form.password,
                onValueChange = onPasswordChange,
                label = { Text("App password") },
                placeholder = {
                    Text(if (form.hasSavedPassword) "Saved. Leave empty to keep it" else "")
                },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = form.folder,
                onValueChange = onFolderChange,
                label = { Text("Folder in Nextcloud") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Button(onClick = onSaveAndTest, enabled = !form.isWorking) {
                Text(if (form.isWorking) "Connecting…" else "Save and test connection")
            }
            form.message?.let { message ->
                Text(text = message, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}