package com.youtube.rating.android.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

internal interface NotificationPermissionRequester {
    /**
     * @return true if caller can proceed immediately; false if a permission prompt flow is in progress.
     */
    fun request(onGranted: () -> Unit): Boolean
}

@Composable
internal fun rememberPostNotificationsPermissionRequester(): NotificationPermissionRequester {
    val context = LocalContext.current

    val pendingOnGranted = remember { mutableStateOf<(() -> Unit)?>(null) }
    var showPermissionDialog by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) pendingOnGranted.value?.invoke()
        pendingOnGranted.value = null
    }

    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = {
                showPermissionDialog = false
                pendingOnGranted.value = null
            },
            title = { Text("Dozvola za obavijesti") },
            text = {
                Text("Da bi obavijesti radile (evanđelje/svetac/novi video), potrebno je odobriti dozvolu za obavijesti.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showPermissionDialog = false
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                ) { Text("Odobri") }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        showPermissionDialog = false
                        pendingOnGranted.value = null
                    }
                ) { Text("Ne sada") }
            }
        )
    }

    return remember(context) {
        object : NotificationPermissionRequester {
            override fun request(onGranted: () -> Unit): Boolean {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
                if (context.hasPermission(Manifest.permission.POST_NOTIFICATIONS)) return true
                pendingOnGranted.value = onGranted
                showPermissionDialog = true
                return false
            }
        }
    }
}

private fun Context.hasPermission(permission: String): Boolean =
    ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

