package com.vayora.extension.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vayora.extension.data.SecurityEvent
import com.vayora.extension.monitor.EventType
import com.vayora.extension.ui.theme.Danger
import com.vayora.extension.ui.theme.Success
import com.vayora.extension.ui.theme.Warning

private val Filters = listOf("All", "Failed attempts", "Unlocks", "Night")

@Composable
fun SecurityActivityScreen(vm: MonitorViewModel, onBack: () -> Unit) {
    val events by vm.events.collectAsStateWithLifecycle()
    var filter by rememberSaveable { mutableStateOf("All") }

    val visible = events.filter { event ->
        when (filter) {
            "Failed attempts" -> event.type == EventType.FAILED_AUTH
            "Unlocks" -> event.type == EventType.UNLOCK
            "Night" -> event.isNight
            else -> true
        }
    }.take(500)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { ScreenHeader("Security Activity", onBack) }
        item {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Filters.forEach { name ->
                    FilterChip(
                        selected = filter == name,
                        onClick = { filter = name },
                        label = { Text(name) }
                    )
                }
            }
        }
        item {
            Text(
                "${visible.size} events, newest first",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (visible.isEmpty()) {
            item {
                Panel(
                    "Nothing recorded yet",
                    "Turn on monitoring in Device Security. Unlocks, failed attempts and " +
                        "restarts then appear here."
                )
            }
        }
        items(visible, key = { it.id }) { event ->
            EventCard(event)
        }
    }
}

@Composable
private fun EventCard(event: SecurityEvent) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                event.type,
                style = MaterialTheme.typography.titleMedium,
                color = if (event.type == EventType.FAILED_AUTH) Danger
                else MaterialTheme.colorScheme.onSurface
            )
            Text(
                formatTimestamp(event.timestamp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (event.details.isNotEmpty()) {
                Text(event.details, style = MaterialTheme.typography.bodySmall)
            }
            if (event.deviceState.isNotEmpty()) {
                Text(
                    event.deviceState,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                if (event.isNight) {
                    Text(
                        "Night activity",
                        style = MaterialTheme.typography.labelMedium,
                        color = Warning
                    )
                }
                Text(
                    if (event.synced) "Uploaded" else "Waiting to upload",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (event.synced) Success else Warning
                )
            }
        }
    }
}
