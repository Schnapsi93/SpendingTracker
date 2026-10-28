package com.aljaz.spendingtracker.ui.settings

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.aljaz.spendingtracker.automation.externalnotifications.ExternalNotificationAccess
import com.aljaz.spendingtracker.ui.components.DayPickerDialog
import com.aljaz.spendingtracker.ui.components.rememberNotificationAccess

/**
 * Settings screen: CSV export and import, and the Nextcloud backup.
 *
 * @param onBackClick called when the user wants to return to Home
 * @param onShowSetupGuideClick called when the user wants to see the setup guide again
 * @param onSharedAccountClick called when the user wants to open the shared expenses login
 * @param viewModel provided by Hilt automatically
 */
@Composable
fun SettingsScreen(
    onBackClick: () -> Unit,
    onShowSetupGuideClick: () -> Unit,
    onSharedAccountClick: () -> Unit,
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
                        text = "Export saves a complete backup (transactions, categories and " +
                                "keyword rules) as one CSV file, semicolon-separated, which opens " +
                                "in Excel and LibreOffice. Import reads such a file, or an older " +
                                "transactions-only file; transactions already in the app are skipped.",
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

            // ----- Time period -----
            PeriodCard(
                monthStartDay = state.monthStartDay,
                carryOver = state.carryOver,
                onMonthStartDayChange = viewModel::onMonthStartDayChange,
                onCarryOverToggle = viewModel::onCarryOverToggle
            )

            // ----- Bank SMS -----
            BankSmsCard(
                test = state.smsTest,
                onTestTextChange = viewModel::onSmsTestTextChange,
                onProcessTest = viewModel::onProcessSmsTestClick
            )

            // ----- External notifications (NLB Pay) -----
            BankAppsCard()

            // ----- Notifications for new transactions -----
            NotificationsCard(
                enabled = state.notificationsEnabled,
                onToggle = viewModel::onNotificationsToggle
            )

            // ----- Shared expenses -----
            SharedExpensesCard(onOpenClick = onSharedAccountClick)

            // ----- Nextcloud backup -----
            CloudBackupCard(
                form = state.cloud,
                onUrlChange = viewModel::onCloudUrlChange,
                onUsernameChange = viewModel::onCloudUsernameChange,
                onPasswordChange = viewModel::onCloudPasswordChange,
                onFolderChange = viewModel::onCloudFolderChange,
                onSaveAndTest = viewModel::onSaveAndTestClick,
                onBackupNow = viewModel::onBackupNowClick
            )

            // ----- Help -----
            TextButton(onClick = onShowSetupGuideClick) {
                Text("Show setup guide")
            }
        }
    }
}

/**
 * Entry point to the shared expenses account (login to the PocketBase server).
 * The account itself is managed on its own screen.
 */
@Composable
private fun SharedExpensesCard(onOpenClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(text = "Shared expenses", style = MaterialTheme.typography.titleMedium)
            Text(
                text = "Log in to the shared server to split expenses with your partner. " +
                        "Only expenses in split categories are ever shared.",
                style = MaterialTheme.typography.bodyMedium
            )
            OutlinedButton(onClick = onOpenClick) { Text("Shared account") }
        }
    }
}

/**
 * The Nextcloud settings form, the backup buttons, and the time of the
 * last successful backup.
 */
@Composable
private fun CloudBackupCard(
    form: CloudFormState,
    onUrlChange: (String) -> Unit,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onFolderChange: (String) -> Unit,
    onSaveAndTest: () -> Unit,
    onBackupNow: () -> Unit
) {
    val isBusy = form.isWorking || form.isBackingUp

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(text = "Nextcloud backup", style = MaterialTheme.typography.titleMedium)
            Text(
                text = "Backs up your transactions, categories and keyword rules once a " +
                        "month as spending_backup.csv, whenever the phone is online. " +
                        "Use an app password (Nextcloud → Personal settings → Security), " +
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

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onSaveAndTest, enabled = !isBusy) {
                    Text(if (form.isWorking) "Connecting…" else "Save and test")
                }
                // Only possible once a password has been saved.
                OutlinedButton(onClick = onBackupNow, enabled = !isBusy && form.hasSavedPassword) {
                    Text(if (form.isBackingUp) "Backing up…" else "Back up now")
                }
            }

            Text(
                text = "Last backup: ${form.lastBackupText ?: "never"}",
                style = MaterialTheme.typography.bodySmall
            )
            form.message?.let { message ->
                Text(text = message, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}


/**
 * Bank SMS card: permission status, a shortcut to grant it, and a field
 * for testing any bank message.
 */
@Composable
private fun BankSmsCard(
    test: SmsTestState,
    onTestTextChange: (String) -> Unit,
    onProcessTest: () -> Unit
) {
    val context = LocalContext.current

    // Permission state is Android-specific UI state, so it lives here.
    var smsAllowed by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS) ==
                    PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted -> smsAllowed = granted }
    )

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(text = "Bank SMS", style = MaterialTheme.typography.titleMedium)
            Text(
                text = "Card transactions from ${com.aljaz.spendingtracker.automation.parser.DhSmsParser.SENDER} " +
                        "are added automatically. Messages from anyone else are ignored and never stored.",
                style = MaterialTheme.typography.bodyMedium
            )

            if (smsAllowed) {
                Text(text = "Status: allowed ✓", style = MaterialTheme.typography.bodyMedium)
            } else {
                Text(text = "Status: not allowed", style = MaterialTheme.typography.bodyMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { permissionLauncher.launch(Manifest.permission.RECEIVE_SMS) }) {
                        Text("Allow reading SMS")
                    }
                    // If Android stops showing the dialog (after refusing twice,
                    // or for restricted settings), the permission is granted here instead.
                    TextButton(onClick = {
                        context.startActivity(
                            Intent(
                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.fromParts("package", context.packageName, null)
                            )
                        )
                    }) {
                        Text("App settings")
                    }
                }
            }

            Text(text = "Test with a message", style = MaterialTheme.typography.labelLarge)
            OutlinedTextField(
                value = test.text,
                onValueChange = onTestTextChange,
                label = { Text("Paste a bank SMS") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedButton(
                onClick = onProcessTest,
                enabled = !test.isProcessing && test.text.isNotBlank()
            ) {
                Text(if (test.isProcessing) "Processing…" else "Process message")
            }
            test.result?.let { result ->
                Text(text = result, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}


/**
 * Notifications card: on/off switch, and on Android 13+ the system
 * permission that notifications need.
 */
@Composable
private fun NotificationsCard(
    enabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val needsPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    // Permission state is Android-specific UI state, so it lives here.
    var permissionGranted by remember {
        mutableStateOf(
            !needsPermission ||
                    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                    PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted -> permissionGranted = granted }
    )

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(text = "Notifications", style = MaterialTheme.typography.titleMedium)

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Notify me about new bank transactions",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f)
                )
                Switch(checked = enabled, onCheckedChange = onToggle)
            }

            if (enabled && !permissionGranted) {
                Text(
                    text = "Android needs your permission to show notifications.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Button(onClick = {
                    if (needsPermission) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }) {
                    Text("Allow notifications")
                }
            }
        }
    }
}


/** Time period card: month start day and carry-over. */
@Composable
private fun PeriodCard(
    monthStartDay: Int,
    carryOver: Boolean,
    onMonthStartDayChange: (Int) -> Unit,
    onCarryOverToggle: (Boolean) -> Unit
) {
    var showPicker by rememberSaveable { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(text = "Time period", style = MaterialTheme.typography.titleMedium)

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Month starts on day",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f)
                )
                OutlinedButton(onClick = { showPicker = true }) {
                    Text(monthStartDay.toString())
                }
            }
            Text(
                text = if (monthStartDay == 1) {
                    "Periods follow the calendar months."
                } else {
                    "Each period runs from day $monthStartDay to day ${monthStartDay - 1} " +
                            "of the next month and is named after the month it starts in."
                },
                style = MaterialTheme.typography.bodySmall
            )

            HorizontalDivider()

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Carry over balance",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f)
                )
                Switch(checked = carryOver, onCheckedChange = onCarryOverToggle)
            }
            Text(
                text = if (carryOver) {
                    "Each period starts with what was left (or missing) from all earlier periods."
                } else {
                    "Each period stands alone."
                },
                style = MaterialTheme.typography.bodySmall
            )
        }
    }

    if (showPicker) {
        DayPickerDialog(
            selected = monthStartDay,
            onDaySelected = { day ->
                onMonthStartDayChange(day)
                showPicker = false
            },
            onDismiss = { showPicker = false }
        )
    }
}

/**
 * Banking apps card: notification access status and a shortcut to grant it.
 * Needed for banks that report card payments through their app instead of
 * SMS (currently NLB Pay). Same layout as [BankSmsCard].
 */

@Composable
private fun BankAppsCard() {
    val context = LocalContext.current

    // Granted state, re-checked after returning from system settings.
    val access = rememberNotificationAccess()

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(text = "Banking apps", style = MaterialTheme.typography.titleMedium)
            Text(
                text = "Card transactions from supported banking apps (NLB Pay) are added " +
                        "automatically from their notifications. Notifications from other " +
                        "apps are ignored and never stored.",
                style = MaterialTheme.typography.bodyMedium
            )

            if (access.granted) {
                Text(text = "Status: allowed ✓", style = MaterialTheme.typography.bodyMedium)
                // Lets the user turn access off again.
                TextButton(onClick = access.request) { Text("Manage notification access") }
            } else {
                Text(text = "Status: not allowed", style = MaterialTheme.typography.bodyMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = access.request) {
                        Text("Allow notification access")
                    }
                    // If the switch is greyed out ("restricted setting"), it has to be
                    // unlocked first in the app's settings: ⋮ → Allow restricted settings.
                    TextButton(onClick = {
                        context.startActivity(
                            Intent(
                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.fromParts("package", context.packageName, null)
                            )
                        )
                    }) {
                        Text("App settings")
                    }
                }
            }
        }
    }
}