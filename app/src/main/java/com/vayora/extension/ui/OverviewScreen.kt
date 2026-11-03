package com.vayora.extension.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.vayora.extension.data.SecurityEvent
import com.vayora.extension.ui.theme.Accent
import com.vayora.extension.ui.theme.Raised
import com.vayora.extension.ui.theme.Warning

@Composable
fun OverviewScreen(
    state: DashboardState,
    onOpen: (Section) -> Unit,
    onLock: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 48.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SentinelLogo(Modifier.width(220.dp).aspectRatio(LogoAspect))
                Spacer(Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Raised)
                        .clickable(onClick = onLock),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Lock, contentDescription = "Lock now", tint = Accent)
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Security overview", style = MaterialTheme.typography.headlineMedium)
                Text(
                    "Your phone, watched over and backed up privately.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        item { LastEventCard(state.lastEvent) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                StatCard("Recorded events", state.eventCount.toString(), Modifier.weight(1f))
                StatCard("Evidence photos", state.photoCount.toString(), Modifier.weight(1f))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                StatCard("Pending uploads", state.pendingUploads.toString(), Modifier.weight(1f))
                StatCard(
                    "Google Drive",
                    state.driveStatus,
                    Modifier.weight(1f),
                    large = false
                )
            }
        }
        item {
            StatCard(
                "Last successful sync",
                state.lastSync?.let { formatTimestamp(it) } ?: "Never",
                Modifier.fillMaxWidth(),
                large = false
            )
        }
        item {
            Text(
                "Sections",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 10.dp)
            )
        }
        items(Sections) { section -> SectionRow(section, onOpen) }
    }
}

@Composable
private fun LastEventCard(event: SecurityEvent?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "LAST RECORDED EVENT",
                style = MaterialTheme.typography.labelMedium,
                color = Accent
            )
            if (event == null) {
                Text(
                    "No security events recorded yet",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    "Turn on monitoring in Device Security to start the timeline.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(event.type, style = MaterialTheme.typography.titleLarge)
                Text(
                    formatTimestamp(event.timestamp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (event.isNight) {
                    Text(
                        "Night activity",
                        style = MaterialTheme.typography.labelMedium,
                        color = Warning
                    )
                }
            }
        }
    }
}

@Composable
private fun StatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    large: Boolean = true
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                value,
                style = if (large) {
                    MaterialTheme.typography.headlineMedium
                } else {
                    MaterialTheme.typography.titleLarge
                }
            )
            Text(
                label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SectionRow(section: Section, onOpen: (Section) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onOpen(section) },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Raised),
                contentAlignment = Alignment.Center
            ) {
                Icon(section.icon, contentDescription = null, tint = Accent)
            }
            Column(
                Modifier.padding(start = 18.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(section.title, style = MaterialTheme.typography.titleMedium)
                Text(
                    section.blurb,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
