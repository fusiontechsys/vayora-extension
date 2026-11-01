package com.vayora.extension.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun PasscodeSettingsScreen(
    onBack: () -> Unit,
    onChange: () -> Unit,
    onLockNow: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text("Passcode Settings", style = MaterialTheme.typography.titleLarge)
        }
        InfoCard(
            "Storage",
            "Your passcode is never stored. Only a salted PBKDF2-SHA256 hash is kept, " +
                "encrypted with a key held in the Android Keystore."
        )
        InfoCard(
            "Relock",
            "The app locks every time you leave it, including when the screen turns off " +
                "or you switch to another app."
        )
        InfoCard(
            "Lockout schedule",
            "5 wrong attempts: 30 seconds\n6: 1 minute\n7: 5 minutes\n" +
                "8: 15 minutes\n9: 1 hour\n10 or more: 24 hours"
        )
        Button(onClick = onChange, modifier = Modifier.fillMaxWidth()) {
            Text("Change passcode")
        }
        OutlinedButton(onClick = onLockNow, modifier = Modifier.fillMaxWidth()) {
            Text("Lock now")
        }
    }
}

@Composable
private fun InfoCard(title: String, body: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
