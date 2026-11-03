package com.vayora.extension.ui

import androidx.compose.foundation.BorderStroke
import com.vayora.extension.ui.theme.ThemeSettings
import androidx.compose.foundation.layout.imePadding
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vayora.extension.ui.theme.Warning

@Composable
fun VayoraRoot(
    dashboard: DashboardViewModel,
    auth: AuthViewModel,
    cloud: CloudViewModel,
    gallery: GalleryViewModel,
    monitor: MonitorViewModel,
    recovery: RecoveryViewModel,
    themeSettings: ThemeSettings
) {
    val authUi by auth.ui.collectAsStateWithLifecycle()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(Modifier.systemBarsPadding().imePadding()) {
            when (authUi.stage) {
                Stage.UNLOCKED -> DashboardHost(dashboard, auth, cloud, gallery, monitor, recovery, themeSettings)
                Stage.RECOVERY -> RecoveryScreen(recovery, auth)
                else -> LockScreen(authUi, auth)
            }
        }
    }
}

@Composable
private fun DashboardHost(
    dashboard: DashboardViewModel,
    auth: AuthViewModel,
    cloud: CloudViewModel,
    gallery: GalleryViewModel,
    monitor: MonitorViewModel,
    recovery: RecoveryViewModel,
    themeSettings: ThemeSettings
) {
    val state by dashboard.state.collectAsStateWithLifecycle()
    val themeMode by themeSettings.mode.collectAsStateWithLifecycle()
    var selected by rememberSaveable { mutableStateOf<String?>(null) }
    val section = Sections.firstOrNull { it.title == selected }

    BackHandler(enabled = section != null) { selected = null }

    Column(Modifier.fillMaxSize()) {
        if (section == null && !state.recoveryReady) {
            RecoveryBanner { selected = "Recovery Settings" }
        }
        Box(Modifier.weight(1f).fillMaxWidth()) {
            if (section == null) {
                OverviewScreen(
                    state = state,
                    onOpen = { selected = it.title },
                    onLock = { auth.lock() }
                )
            } else if (section.title == "Passcode Settings") {
                PasscodeSettingsScreen(
                    onBack = { selected = null },
                    onChange = { auth.startChange() },
                    onLockNow = { auth.lock() }
                )
            } else if (section.title == "Google Account") {
                GoogleAccountScreen(cloud) { selected = null }
            } else if (section.title == "Cloud Evidence") {
                CloudEvidenceScreen(cloud) { selected = null }
            } else if (section.title == "Photo Gallery") {
                PhotoGalleryScreen(gallery) { selected = null }
            } else if (section.title == "Device Security") {
                DeviceSecurityScreen(monitor) { selected = null }
            } else if (section.title == "Security Activity") {
                SecurityActivityScreen(monitor) { selected = null }
            } else if (section.title == "Recovery Settings") {
                RecoverySettingsScreen(recovery) { selected = null }
            } else if (section.title == "Application Settings") {
                AppSettingsScreen(
                    themeMode = themeMode,
                    onThemeMode = { themeSettings.setMode(it) },
                    onBack = { selected = null },
                    onSecureLogout = {
                        cloud.disconnect()
                        gallery.clear()
                        auth.lock()
                    }
                )
            } else {
                SectionScreen(section) { selected = null }
            }
        }
    }
}

@Composable
private fun RecoveryBanner(onSetUp: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 10.dp),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Passcode recovery is not ready. Without it a forgotten passcode cannot " +
                    "be reset.",
                style = MaterialTheme.typography.bodySmall,
                color = Warning,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onSetUp) { Text("Set up") }
        }
    }
}

@Composable
fun SectionScreen(section: Section, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text(section.title, style = MaterialTheme.typography.titleLarge)
        }
        Spacer(Modifier.height(16.dp))
        Text(
            "This section is built in Phase ${section.phase}.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
