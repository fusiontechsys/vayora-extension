package com.vayora.extension.ui

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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch

@Composable
fun RecoverySettingsScreen(vm: RecoveryViewModel, onBack: () -> Unit) {
    val hasPhrase by vm.hasPhrase.collectAsStateWithLifecycle()
    val email by vm.driveEmail.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    var current by remember { mutableStateOf("") }
    var newPhrase by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var saved by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        ScreenHeader("Recovery Settings", onBack)
        Panel(
            "How recovery works",
            "If you forget your passcode, you verify your registered Google account and " +
                "then enter your recovery phrase. Both are required. Without a recovery " +
                "phrase, a forgotten passcode can only be fixed by reinstalling the app."
        )
        Panel(
            "Google account",
            email ?: "Not connected. Connect it in Google Account first.",
            email == null
        )
        Panel(
            "Recovery phrase",
            if (hasPhrase) "Set. Stored only as a salted hash." else "Not set yet.",
            !hasPhrase
        )

        if (hasPhrase) {
            OutlinedTextField(
                value = current,
                onValueChange = { current = it },
                label = { Text("Current recovery phrase") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth()
            )
        }
        OutlinedTextField(
            value = newPhrase,
            onValueChange = { newPhrase = it },
            label = { Text(if (hasPhrase) "New recovery phrase" else "Recovery phrase") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = confirm,
            onValueChange = { confirm = it },
            label = { Text("Repeat recovery phrase") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth()
        )
        error?.let { Panel("Problem", it, true) }
        if (saved) {
            Panel("Saved", "Your recovery phrase is saved. Keep a written copy somewhere safe.")
        }
        Button(
            onClick = {
                scope.launch {
                    busy = true
                    saved = false
                    error = vm.changePhrase(current, newPhrase, confirm)
                    if (error == null) {
                        saved = true
                        current = ""
                        newPhrase = ""
                        confirm = ""
                    }
                    busy = false
                }
            },
            enabled = !busy && newPhrase.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) { Text(if (busy) "Saving..." else if (hasPhrase) "Change phrase" else "Save phrase") }
    }
}
