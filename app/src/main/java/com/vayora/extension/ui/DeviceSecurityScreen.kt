package com.vayora.extension.ui

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vayora.extension.monitor.AdminHelper
import com.vayora.extension.monitor.MonitorService
import com.vayora.extension.ui.theme.Success
import com.vayora.extension.ui.theme.Warning

private enum class Tone { GOOD, WARN, NONE }

@Composable
fun DeviceSecurityScreen(vm: MonitorViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val config by vm.settings.collectAsStateWithLifecycle()
    val running by MonitorService.running.collectAsStateWithLifecycle()
    var adminActive by remember { mutableStateOf(AdminHelper.isActive(context)) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        adminActive = AdminHelper.isActive(context)
    }

    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> vm.setMonitoring(true) }

    val toggleMonitoring: (Boolean) -> Unit = { enable ->
        if (!enable) {
            vm.setMonitoring(false)
        } else if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            vm.setMonitoring(true)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        ScreenHeader("Device Security", onBack)

        SettingCard(
            title = "Security monitoring",
            subtitle = if (running) {
                "Running. A small silent notification stays in the status bar."
            } else {
                "Stopped. Turn on to record unlocks and restarts."
            },
            checked = config.enabled,
            onChange = toggleMonitoring
        )
        SettingCard(
            title = "Record successful unlocks",
            subtitle = "Adds an entry every time the phone is unlocked.",
            checked = config.recordUnlocks,
            onChange = { vm.setRecordUnlocks(it) }
        )

        if (adminActive) {
            OutlinedButton(
                onClick = {
                    AdminHelper.remove(context)
                    adminActive = false
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Turn off failed-attempt detection") }
        } else {
            Button(
                onClick = { context.startActivity(AdminHelper.requestIntent(context)) },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Turn on failed-attempt detection") }
        }
        OutlinedButton(
            onClick = {
                try {
                    context.startActivity(
                        Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                    )
                } catch (e: ActivityNotFoundException) {
                    // settings screen not available on this device
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Battery settings (set Vayora to Unrestricted)") }

        Text(
            "What Android allows",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 6.dp)
        )
        CapabilityCard(
            "Screen unlocks",
            if (running && config.recordUnlocks) "Active" else "Off",
            if (running && config.recordUnlocks) Tone.GOOD else Tone.WARN,
            "Android announces that the phone was unlocked. It does not say whether a " +
                "PIN, pattern or fingerprint was used."
        )
        CapabilityCard(
            "Failed unlock attempts",
            if (adminActive) "Active" else "Needs setup",
            if (adminActive) Tone.GOOD else Tone.WARN,
            "Reported for wrong PIN, pattern or password through Device Admin approval. " +
                "Fingerprint and face failures are generally not reported by Android."
        )
        CapabilityCard(
            "Device restarts",
            if (running) "Active" else "Off",
            if (running) Tone.GOOD else Tone.WARN,
            "Recorded once you unlock the phone after a restart."
        )
        CapabilityCard(
            "Night activity",
            "Active",
            Tone.GOOD,
            "Events between 22:00 and 06:00 are flagged in the activity timeline."
        )
        CapabilityCard(
            "Which method unlocked the phone",
            "Not available",
            Tone.NONE,
            "Android does not tell ordinary apps how the phone was unlocked."
        )
        CapabilityCard(
            "Background photo on a successful unlock",
            "Not possible",
            Tone.NONE,
            "Android 11 and newer block camera use from the background, so Vayora " +
                "cannot silently photograph an unlock."
        )
    }
}

@Composable
private fun SettingCard(
    title: String,
    subtitle: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(checked = checked, onCheckedChange = onChange)
        }
    }
}

@Composable
private fun CapabilityCard(name: String, status: String, tone: Tone, note: String) {
    val statusColor: Color = when (tone) {
        Tone.GOOD -> Success
        Tone.WARN -> Warning
        Tone.NONE -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    name,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                Text(status, style = MaterialTheme.typography.labelMedium, color = statusColor)
            }
            Text(
                note,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
