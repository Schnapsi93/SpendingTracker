package com.aljaz.spendingtracker.ui.onboarding

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aljaz.spendingtracker.automation.parser.DhSmsParser
import com.aljaz.spendingtracker.ui.components.DayPickerDialog
import com.aljaz.spendingtracker.ui.components.openAppSettings
import com.aljaz.spendingtracker.ui.components.rememberNotificationAccess
import com.aljaz.spendingtracker.ui.components.rememberPermissionRequest

/** The pages of the guide, in order. */
private enum class OnboardingPage { WELCOME, SMS, BANK_APPS, NOTIFICATIONS, PERIOD, BACKUP }

/**
 * First-launch setup guide: one topic per page, every page optional.
 *
 * @param onFinished called when the guide ends; true means "open the Settings
 *        screen next" (from the backup page), false means "go to Home"
 * @param viewModel provided by Hilt automatically
 */
@Composable
fun OnboardingScreen(
    onFinished: (openSettings: Boolean) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    // The notifications page only exists where the permission does (Android 13+).
    val pages = remember {
        OnboardingPage.entries.filter {
            it != OnboardingPage.NOTIFICATIONS || Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
        }
    }
    var index by rememberSaveable { mutableIntStateOf(0) }
    val page = pages[index]
    val isLast = index == pages.lastIndex

    val monthStartDay by viewModel.monthStartDay.collectAsState()
    val carryOver by viewModel.carryOver.collectAsState()

    /** Marks the guide as done, then leaves it. */
    fun finish(openSettings: Boolean) {
        viewModel.onFinish()
        onFinished(openSettings)
    }

    // The phone's back gesture goes to the previous page instead of leaving the guide.
    BackHandler(enabled = index > 0) { index-- }

    Scaffold(
        modifier = modifier,
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (index == 0) {
                    TextButton(onClick = { finish(openSettings = false) }) { Text("Skip") }
                } else {
                    TextButton(onClick = { index-- }) { Text("Back") }
                }
                Spacer(modifier = Modifier.weight(1f))
                if (isLast) {
                    Button(onClick = { finish(openSettings = false) }) { Text("Finish") }
                } else {
                    Button(onClick = { index++ }) { Text("Next") }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Step ${index + 1} of ${pages.size}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )

            when (page) {
                OnboardingPage.WELCOME -> WelcomePage()
                OnboardingPage.SMS -> SmsPage()
                OnboardingPage.BANK_APPS -> BankAppsPage()
                OnboardingPage.NOTIFICATIONS -> NotificationsPage()
                OnboardingPage.PERIOD -> PeriodPage(
                    monthStartDay = monthStartDay,
                    carryOver = carryOver,
                    onMonthStartDayChange = viewModel::onMonthStartDayChange,
                    onCarryOverToggle = viewModel::onCarryOverToggle
                )
                OnboardingPage.BACKUP -> BackupPage(
                    onSetUpNow = { finish(openSettings = true) }
                )
            }
        }
    }
}

// ---------- Pages ----------

@Composable
private fun WelcomePage() {
    PageTitle("Welcome to Spending Tracker")
    PageText(
        "Track your income and expenses month by month. Card payments from your " +
                "bank's SMS or banking app are added and categorized automatically, and " +
                "your history can be backed up to your own Nextcloud."
    )
    PageText("This short guide sets up the essentials. Every step is optional and can be changed later in Settings.")
}

@Composable
private fun SmsPage() {
    val sms = rememberPermissionRequest(Manifest.permission.RECEIVE_SMS)
    val context = LocalContext.current

    PageTitle("Bank SMS")
    PageText(
        "Card payments from ${DhSmsParser.SENDER} are added automatically and sorted " +
                "into categories with your keyword rules. Messages from anyone else are " +
                "ignored and never stored."
    )
    if (sms.granted) {
        StatusText("Reading SMS is allowed ✓")
    } else {
        Button(onClick = sms.request) { Text("Allow reading SMS") }
        PageText(
            "If no dialog appears, allow SMS in the app's settings. On some phones " +
                    "you first need ⋮ → Allow restricted settings there.",
            small = true
        )
        TextButton(onClick = { context.openAppSettings() }) { Text("Open app settings") }
    }
}

/**
 * Notification access for banks that report card payments through their
 * app instead of SMS (currently NLB Pay). Same layout as [SmsPage].
 */
@Composable
private fun BankAppsPage() {
    val access = rememberNotificationAccess()
    val context = LocalContext.current

    PageTitle("Banking apps")
    PageText(
        "Some banks report card payments as a notification from their app instead " +
                "of an SMS (currently supported: NLB Pay). With notification access, " +
                "these are added automatically, just like bank SMS. Notifications from " +
                "all other apps are ignored and never stored."
    )
    if (access.granted) {
        StatusText("Notification access is allowed ✓")
    } else {
        Button(onClick = access.request) { Text("Allow notification access") }
        PageText(
            "Find Spending Tracker in the list and switch it on. If the switch is " +
                    "greyed out, first choose ⋮ → Allow restricted settings in the app's settings.",
            small = true
        )
        TextButton(onClick = { context.openAppSettings() }) { Text("Open app settings") }
    }
}

@Composable
private fun NotificationsPage() {
    val notifications = rememberPermissionRequest(
        permission = Manifest.permission.POST_NOTIFICATIONS,
        requiredFromSdk = Build.VERSION_CODES.TIRAMISU
    )

    PageTitle("Notifications")
    PageText(
        "Get a short notification whenever a bank payment is added, showing the " +
                "amount, the shop and its category. Tap it to review the transaction."
    )
    if (notifications.granted) {
        StatusText("Notifications are allowed ✓")
    } else {
        Button(onClick = notifications.request) { Text("Allow notifications") }
    }
}

@Composable
private fun PeriodPage(
    monthStartDay: Int,
    carryOver: Boolean,
    onMonthStartDayChange: (Int) -> Unit,
    onCarryOverToggle: (Boolean) -> Unit
) {
    var showPicker by rememberSaveable { mutableStateOf(false) }

    PageTitle("Your month")
    PageText(
        "If you plan your budget from payday to payday, let your month start on that " +
                "day. A period then runs, for example, from the 15th to the 14th and is named " +
                "after the month it starts in."
    )
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "Month starts on day",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        OutlinedButton(onClick = { showPicker = true }) { Text(monthStartDay.toString()) }
    }

    PageText(
        "With carry-over, each month starts with what was left (or missing) from the months before."
    )
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "Carry over balance",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        Switch(checked = carryOver, onCheckedChange = onCarryOverToggle)
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

@Composable
private fun BackupPage(onSetUpNow: () -> Unit) {
    PageTitle("Backup")
    PageText(
        "Once a month, the app can upload a complete backup (transactions, categories " +
                "and keyword rules) to your Nextcloud. You need the server address, your " +
                "username and an app password."
    )
    OutlinedButton(onClick = onSetUpNow) { Text("Set up backup now") }
    PageText("You can also do this later under Settings → Nextcloud backup.", small = true)
}

// ---------- Small building blocks ----------

@Composable
private fun PageTitle(text: String) {
    Text(text = text, style = MaterialTheme.typography.headlineSmall)
}

@Composable
private fun PageText(text: String, small: Boolean = false) {
    Text(
        text = text,
        style = if (small) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyLarge
    )
}

@Composable
private fun StatusText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.primary
    )
}