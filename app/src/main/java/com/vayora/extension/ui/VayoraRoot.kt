package com.vayora.extension.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun VayoraRoot(dashboard: DashboardViewModel, auth: AuthViewModel) {
    val authUi by auth.ui.collectAsStateWithLifecycle()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(Modifier.systemBarsPadding()) {
            if (authUi.stage == Stage.UNLOCKED) {
                DashboardHost(dashboard, auth)
            } else {
                LockScreen(authUi, auth)
            }
        }
    }
}

@Composable
private fun DashboardHost(dashboard: DashboardViewModel, auth: AuthViewModel) {
    val state by dashboard.state.collectAsStateWithLifecycle()
    var selected by rememberSaveable { mutableStateOf<String?>(null) }
    val section = Sections.firstOrNull { it.title == selected }

    BackHandler(enabled = section != null) { selected = null }

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
    } else {
        SectionScreen(section) { selected = null }
    }
}

@Composable
fun SectionScreen(section: Section, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(20.dp)) {
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
