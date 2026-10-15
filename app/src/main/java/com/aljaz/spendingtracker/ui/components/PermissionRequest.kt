package com.aljaz.spendingtracker.ui.components

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

/**
 * The state of one Android permission, plus a way to ask for it.
 *
 * @property granted true if the app has the permission (or doesn't need it on this Android version)
 * @property request shows the system permission dialog
 */
class PermissionRequest(
    val granted: Boolean,
    val request: () -> Unit
)

/**
 * Remembers whether [permission] is granted and prepares the system dialog for it.
 *
 * @param requiredFromSdk the Android version that introduced the permission;
 *        on older versions it counts as granted (e.g. notifications before Android 13)
 */
@Composable
fun rememberPermissionRequest(
    permission: String,
    requiredFromSdk: Int = Build.VERSION_CODES.BASE
): PermissionRequest {
    val context = LocalContext.current
    val needed = Build.VERSION.SDK_INT >= requiredFromSdk

    var granted by remember {
        mutableStateOf(
            !needed || ContextCompat.checkSelfPermission(context, permission) ==
                    PackageManager.PERMISSION_GRANTED
        )
    }
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { result -> granted = result }
    )

    return PermissionRequest(
        granted = granted,
        request = { if (needed) launcher.launch(permission) }
    )
}

/**
 * Opens this app's page in the Android settings, where permissions can be
 * granted when the dialog no longer appears (or for restricted settings).
 */
fun Context.openAppSettings() {
    startActivity(
        Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", packageName, null)
        )
    )
}