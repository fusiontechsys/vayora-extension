package com.vayora.extension.ui

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.ApiException
import com.vayora.extension.cloud.GoogleAuth

@Composable
fun RecoveryScreen(vm: RecoveryViewModel, auth: AuthViewModel) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var phrase by remember { mutableStateOf("") }

    LaunchedEffect(Unit) { vm.reset() }
    BackHandler { auth.cancelRecovery() }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        val data = result.data
        if (result.resultCode == Activity.RESULT_OK && data != null) {
            try {
                val granted = Identity.getAuthorizationClient(context)
                    .getAuthorizationResultFromIntent(data)
                val token = granted.accessToken
                if (token != null) {
                    vm.onGoogleToken(token)
                } else {
                    vm.report("Google did not return an access token.")
                }
            } catch (e: ApiException) {
                vm.report("Google sign-in failed (code ${e.statusCode}).")
            }
        } else {
            vm.report("Google sign-in was cancelled.")
        }
    }

    val verifyWithGoogle: () -> Unit = {
        Identity.getAuthorizationClient(context)
            .authorize(GoogleAuth.request(ui.email))
            .addOnSuccessListener { result ->
                if (result.hasResolution()) {
                    val pending = result.pendingIntent
                    if (pending != null) {
                        launcher.launch(IntentSenderRequest.Builder(pending.intentSender).build())
                    } else {
                        vm.report("Google could not open the sign-in screen.")
                    }
                } else {
                    val token = result.accessToken
                    if (token != null) {
                        vm.onGoogleToken(token)
                    } else {
                        vm.report("Google did not return an access token.")
                    }
                }
            }
            .addOnFailureListener { error ->
                vm.report("Google sign-in failed: ${error.message}")
            }
    }

    val unavailable = vm.unavailableReason()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Recover passcode", style = androidx.compose.material3.MaterialTheme.typography.titleLarge)

        if (unavailable != null) {
            Panel("Recovery is not available", unavailable, true)
        } else if (ui.step == RecoveryStep.GOOGLE) {
            Panel(
                "Step 1 of 2: verify your Google account",
                "Sign in with the registered account (${ui.email}). Google's approval is " +
                    "required but is not enough on its own. You will also need your " +
                    "recovery phrase."
            )
            ui.message?.let { Panel("Problem", it, true) }
            Button(
                onClick = verifyWithGoogle,
                enabled = !ui.busy,
                modifier = Modifier.fillMaxWidth()
            ) { Text(if (ui.busy) "Checking..." else "Verify with Google") }
        } else {
            Panel(
                "Step 2 of 2: recovery phrase",
                "Enter the recovery phrase you created in Recovery Settings."
            )
            if (ui.lockSeconds > 0) {
                val minutes = ui.lockSeconds / 60
                val seconds = ui.lockSeconds % 60
                Panel(
                    "Too many attempts",
                    "Try again in $minutes:${seconds.toString().padStart(2, '0')}",
                    true
                )
            } else {
                ui.message?.let { Panel("Problem", it, true) }
            }
            OutlinedTextField(
                value = phrase,
                onValueChange = { phrase = it },
                label = { Text("Recovery phrase") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = {
                    val entered = phrase
                    phrase = ""
                    vm.verifyPhrase(entered) { auth.recoveryVerified() }
                },
                enabled = !ui.busy && ui.lockSeconds <= 0 && phrase.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) { Text(if (ui.busy) "Checking..." else "Continue") }
        }

        TextButton(onClick = { auth.cancelRecovery() }) { Text("Cancel") }
    }
}
