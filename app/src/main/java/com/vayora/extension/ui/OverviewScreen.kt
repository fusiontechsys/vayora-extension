package com.vayora.extension.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vayora.extension.data.SecurityEvent
import com.vayora.extension.ui.theme.Blue300
import com.vayora.extension.ui.theme.Blue500
import com.vayora.extension.ui.theme.Warning
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private fun formatTime(ms: Long): String =
    SimpleDateFormat("dd MMM yyyy, HH:mm:ss", Locale.getDefault()).format(Date(ms))

@Composable
fun OverviewScreen(state: DashboardState, onOpen: (Section) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column {
                Text(
                    "VAYORA EXTENSION",
                    style = MaterialTheme.typography.labelMedium,
                    color = Blue300
                )
                Text("Security Overview", style = MaterialTheme.typography.headlineMedium)
            }
        }
        item { LastEventCard(state.lastEvent) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard("Recorded events", state.eventCount.toString(), Modifier.weight(1f))
                StatCard("Evidence photos", state.photoCount.toString(), Modifier.weight(1f))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard("Pending uploads", state.pendingUploads.toString(), Modifier.weight(1f))
                StatCard("Google Drive", state.driveStatus, Modifier.weight(1f))
            }
        }
        item {
            StatCard(
                "Last successful sync",
                state.lastSync?.let { formatTime(it) } ?: "Never",
                Modifier.fillMaxWidth()
            )
        }
        item { Text("Sections", style = MaterialTheme.typography.titleMedium) }
        items(Sections) { section -> SectionRow(section, onOpen) }
    }
}

@Composable
private fun LastEventCard(event: SecurityEvent?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                "Last recorded security event",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (event == null) {
                Text(
                    "No security events recorded yet",
                    style = MaterialTheme.typography.titleMedium
                )
            } else {
                Text(event.type, style = MaterialTheme.typography.titleMedium)
                Text(
                    formatTime(event.timestamp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (event.isNight) {
                    Text(
                        "Night activity",
                        style = MaterialTheme.typography.bodySmall,
                        color = Warning
                    )
                }
            }
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(value, style = MaterialTheme.typography.titleLarge)
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
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(section.icon, contentDescription = null, tint = Blue500)
            Column(Modifier.padding(start = 16.dp)) {
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
