package com.vayora.extension.ui

import androidx.compose.foundation.BorderStroke
import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vayora.extension.evidence.CaptureResult
import com.vayora.extension.monitor.AdminHelper
import com.vayora.extension.monitor.MonitorService
import com.vayora.extension.ui.theme.Success
import com.vayora.extension.ui.theme.Warning
import kotlinx.coroutines.launch

private enum class Tone { GOOD, WARN, NONE }

private val GapOptions = listOf(0 to "Every unlock", 1 to "1 min", 5 to "5 min", 30 to "30 min")

private fun hasCameraPermission(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
        PackageManager.PERMISSION_GRANTED

@Composable
fun DeviceSecurityScreen(vm: MonitorViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val config by vm.settings.collectAsStateWithLifecycle()
    val evidence by vm.evidenceSettings.collectAsStateWithLifecycle()
    val running by MonitorService.running.collectAsStateWithLifecycle()
    val cameraActive by MonitorService.cameraActive.collectAsStateWithLifecycle()
    var adminActive by remember { mutableStateOf(AdminHelper.isActive(context)) }
    var cameraGranted by remember { mutableStateOf(hasCameraPermission(context)) }
    var overlayGranted by remember { mutableStateOf(Settings.canDrawOverlays(context)) }
    var testMessage by remember { mutableStateOf<String?>(null) }
    var testing by remember { mutableStateOf(false) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        adminActive = AdminHelper.isActive(context)
        cameraGranted = hasCameraPermission(context)
        overlayGranted = Settings.canDrawOverlays(context)
    }

    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> vm.setMonitoring(true) }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        cameraGranted = granted
        if (granted) vm.restartMonitoring()
    }

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
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
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
            VOutlinedButton(
                onClick = {
                    AdminHelper.remove(context)
                    adminActive = false
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Turn off failed-attempt detection") }
        } else {
            VButton(
                onClick = { context.startActivity(AdminHelper.requestIntent(context)) },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Turn on failed-attempt detection") }
        }
        VOutlinedButton(
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
        ) { Text("Battery settings (set Sentinel to Unrestricted)") }

        Text(
            "Camera evidence",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 6.dp)
        )
        CapabilityCard(
            "Camera permission",
            if (cameraGranted) "Granted" else "Not granted",
            if (cameraGranted) Tone.GOOD else Tone.WARN,
            "Photos use the front camera and are stored privately on this phone until " +
                "they upload to your Google Drive. Android shows its camera indicator " +
                "dot during every capture."
        )
        if (!cameraGranted) {
            VButton(
                onClick = { cameraLauncher.launch(Manifest.permission.CAMERA) },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Allow camera") }
        }
        CapabilityCard(
            "Background camera service",
            if (cameraActive) "Ready" else "Not ready",
            if (cameraActive) Tone.GOOD else Tone.WARN,
            "Becomes ready when monitoring is on, the camera is allowed and a photo " +
                "option below is enabled, after you open this app. After a phone restart, " +
                "open Sentinel once to make it ready again."
        )
        CapabilityCard(
            "Fallback capture screen",
            if (overlayGranted) "Allowed" else "Not allowed",
            if (overlayGranted) Tone.GOOD else Tone.WARN,
            "If the background service cannot use the camera, Vayora briefly shows a " +
                "visible capture screen instead. That needs \"Display over other apps\"."
        )
        if (!overlayGranted) {
            VOutlinedButton(
                onClick = {
                    try {
                        context.startActivity(
                            Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:${context.packageName}")
                            )
                        )
                    } catch (e: ActivityNotFoundException) {
                        // settings screen not available on this device
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Allow display over other apps") }
        }

        SettingCard(
            title = "Photo on every successful unlock",
            subtitle = "Takes a front-camera photo each time the phone is unlocked, " +
                "whoever unlocks it.",
            checked = evidence.captureOnUnlock,
            onChange = { vm.setCaptureOnUnlock(it) }
        )
        if (evidence.captureOnUnlock) {
            Text(
                "Minimum time between unlock photos",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                GapOptions.forEach { option ->
                    FilterChip(
                        selected = evidence.unlockGapMinutes == option.first,
                        onClick = { vm.setUnlockGapMinutes(option.first) },
                        label = { Text(option.second) }
                    )
                }
            }
        }
        SettingCard(
            title = "Photo after every failed phone unlock",
            subtitle = "Needs failed-attempt detection. One photo per failed attempt, at " +
                "most one every 8 seconds.",
            checked = evidence.captureOnSystemFail,
            onChange = { vm.setCaptureOnSystemFail(it) }
        )
        SettingCard(
            title = "Photo after every wrong Sentinel passcode",
            subtitle = "Taken on this lock screen, at most one every 8 seconds.",
            checked = evidence.captureOnAppFail,
            onChange = { vm.setCaptureOnAppFail(it) }
        )

        VButton(
            onClick = {
                scope.launch {
                    testing = true
                    val result = vm.testPhoto(context, owner)
                    testMessage = describe(result)
                    testing = false
                }
            },
            enabled = cameraGranted && !testing,
            modifier = Modifier.fillMaxWidth()
        ) { Text(if (testing) "Taking photo..." else "Take test photo now") }
        VOutlinedButton(
            onClick = {
                vm.scheduleLockScreenTest()
                testMessage = "Test started. Lock your phone now. In about 15 seconds a " +
                    "Vayora screen should appear over the lock screen and take a photo."
            },
            enabled = cameraGranted,
            modifier = Modifier.fillMaxWidth()
        ) { Text("Test lock-screen photo (lock phone within 15 s)") }
        testMessage?.let { Panel("Test result", it) }

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
    }
}

private fun describe(result: CaptureResult): String = when (result) {
    CaptureResult.SUCCESS ->
        "Photo recorded. Open Photo Gallery to see it. It uploads when you are online."
    CaptureResult.NO_PERMISSION -> "Camera permission is not granted."
    CaptureResult.NO_CAMERA -> "This phone has no usable front camera."
    CaptureResult.FAILED -> "The camera could not take the photo. Try again."
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
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
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
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
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
