package com.vayora.extension.ui

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.runtime.DisposableEffect
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.ApiException
import com.vayora.extension.cloud.GoogleAuth

@Composable
fun GoogleAccountScreen(vm: CloudViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val drive by vm.drive.collectAsStateWithLifecycle()
    val message by vm.message.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    var confirmDisconnect by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose { vm.clearMessage() }
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        val data = result.data
        if (result.resultCode == Activity.RESULT_OK && data != null) {
            try {
                val auth = Identity.getAuthorizationClient(context)
                    .getAuthorizationResultFromIntent(data)
                val token = auth.accessToken
                if (token != null) {
                    vm.onToken(token)
                } else {
                    vm.report("Google did not return an access token.")
                }
            } catch (e: ApiException) {
                vm.report("Authorization failed (code ${e.statusCode}).")
            }
        } else {
            vm.report("Connection cancelled.")
        }
    }

    val connect: () -> Unit = {
        Identity.getAuthorizationClient(context)
            .authorize(GoogleAuth.request(drive.email))
            .addOnSuccessListener { result ->
                if (result.hasResolution()) {
                    val pending = result.pendingIntent
                    if (pending != null) {
                        launcher.launch(IntentSenderRequest.Builder(pending.intentSender).build())
                    } else {
                        vm.report("Google could not open the approval screen.")
                    }
                } else {
                    val token = result.accessToken
                    if (token != null) {
                        vm.onToken(token)
                    } else {
                        vm.report("Google did not return an access token.")
                    }
                }
            }
            .addOnFailureListener { error ->
                vm.report("Authorization failed: ${error.message}")
            }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        ScreenHeader("Google Account", onBack)
        Panel("Registered account", drive.email ?: "No account registered yet")
        Panel(
            "Permission",
            "Vayora Extension asks only for the Google Drive drive.file permission. " +
                "It can see only files and folders it creates itself, never the rest of " +
                "your Drive. Your Google password is never seen or stored by this app."
        )
        drive.lastError?.let { Panel("Needs attention", it, true) }
        message?.let { Panel("Status", it) }

        if (drive.email == null) {
            Button(
                onClick = connect,
                enabled = !busy,
                modifier = Modifier.fillMaxWidth()
            ) { Text(if (busy) "Connecting..." else "Connect Google account") }
        } else {
            Button(
                onClick = connect,
                enabled = !busy,
                modifier = Modifier.fillMaxWidth()
            ) { Text(if (busy) "Working..." else "Reconnect account") }
            OutlinedButton(
                onClick = { confirmDisconnect = true },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Disconnect") }
        }
    }

    if (confirmDisconnect) {
        AlertDialog(
            onDismissRequest = { confirmDisconnect = false },
            title = { Text("Disconnect Google account?") },
            text = {
                Text(
                    "Uploads stop and the registered account is forgotten on this phone. " +
                        "Files already in Google Drive are not deleted. To fully revoke " +
                        "access, remove Vayora Extension in your Google Account permissions."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmDisconnect = false
                    vm.disconnect()
                }) { Text("Disconnect") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDisconnect = false }) { Text("Cancel") }
            }
        )
    }
}
