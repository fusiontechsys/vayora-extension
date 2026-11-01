package com.vayora.extension.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.runtime.DisposableEffect
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun CloudEvidenceScreen(vm: CloudViewModel, onBack: () -> Unit) {
    val drive by vm.drive.collectAsStateWithLifecycle()
    val pending by vm.pending.collectAsStateWithLifecycle()
    val message by vm.message.collectAsStateWithLifecycle()
    val connected = drive.email != null

    DisposableEffect(Unit) {
        onDispose { vm.clearMessage() }
    }

    val lastSyncText = drive.lastSync?.let { formatTimestamp(it) } ?: "Never"
    val statusText = if (drive.syncing) "Syncing now" else "Idle"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        ScreenHeader("Cloud Evidence", onBack)
        Panel(
            "Google Drive",
            if (connected) {
                "Connected as ${drive.email}\nFolder: Vayora Extension Evidence"
            } else {
                "Not connected. Open Google Account to connect."
            }
        )
        Panel(
            "Synchronization",
            "Pending uploads: $pending\nLast successful sync: $lastSyncText\nStatus: $statusText"
        )
        drive.lastError?.let { Panel("Needs attention", it, true) }
        message?.let { Panel("Status", it) }

        Button(
            onClick = { vm.syncNow() },
            enabled = connected,
            modifier = Modifier.fillMaxWidth()
        ) { Text("Sync now") }
        OutlinedButton(
            onClick = { vm.recordTestEvent() },
            enabled = connected,
            modifier = Modifier.fillMaxWidth()
        ) { Text("Record test event and upload") }
    }
}
