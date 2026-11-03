package com.vayora.extension.ui

import androidx.compose.foundation.BorderStroke
import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vayora.extension.ui.theme.Danger
import com.vayora.extension.ui.theme.Raised
import com.vayora.extension.ui.theme.Success
import com.vayora.extension.ui.theme.Warning

private data class ThumbState(val bitmap: Bitmap?, val done: Boolean)

@Composable
fun PhotoGalleryScreen(vm: GalleryViewModel, onBack: () -> Unit) {
    val items by vm.items.collectAsStateWithLifecycle()
    val cloud by vm.cloudState.collectAsStateWithLifecycle()
    val message by vm.message.collectAsStateWithLifecycle()
    var filter by rememberSaveable { mutableStateOf<String?>(null) }
    var viewingKey by rememberSaveable { mutableStateOf<String?>(null) }
    var deleteTarget by remember { mutableStateOf<GalleryItem?>(null) }

    LaunchedEffect(Unit) {
        if (!cloud.loadedOnce) vm.refresh()
    }
    DisposableEffect(Unit) {
        onDispose { vm.clearMessage() }
    }

    val viewing = items.firstOrNull { it.key == viewingKey }
    BackHandler(enabled = viewing != null) { viewingKey = null }

    if (viewing != null) {
        PhotoViewer(
            vm = vm,
            item = viewing,
            onClose = { viewingKey = null },
            onDelete = { deleteTarget = it }
        )
    } else {
        val categories = remember(items) { items.map { it.category }.distinct().sorted() }
        val visible = items.filter { filter == null || it.category == filter }

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(24.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                ScreenHeader("Photo Gallery", onBack)
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    VOutlinedButton(onClick = { vm.refresh() }, enabled = !cloud.loading) {
                        Text(if (cloud.loading) "Refreshing..." else "Refresh")
                    }
                    Text(
                        "${visible.size} photos",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (cloud.loading) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                }
            }
            cloud.error?.let { error ->
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Panel("Gallery problem", error, true)
                }
            }
            message?.let { text ->
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Panel("Status", text)
                }
            }
            if (categories.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        FilterChip(
                            selected = filter == null,
                            onClick = { filter = null },
                            label = { Text("All") }
                        )
                        categories.forEach { category ->
                            FilterChip(
                                selected = filter == category,
                                onClick = { filter = category },
                                label = { Text(category) }
                            )
                        }
                    }
                }
            }
            if (visible.isEmpty() && !cloud.loading && cloud.error == null) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Panel(
                        "No evidence photos yet",
                        "Photos captured by Sentinel appear here after they upload " +
                            "to your private Google Drive folder."
                    )
                }
            }
            items(visible, key = { it.key }) { item ->
                PhotoCell(vm, item) { viewingKey = item.key }
            }
        }
    }

    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Delete evidence?") },
            text = {
                Text(
                    if (target.inCloud) {
                        "This permanently deletes the photo from your Google Drive. " +
                            "It cannot be undone."
                    } else {
                        "This deletes the photo stored on this phone before it was " +
                            "uploaded. It cannot be undone."
                    }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.delete(target)
                    deleteTarget = null
                    viewingKey = null
                }) { Text("Delete", color = Danger) }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun PhotoCell(vm: GalleryViewModel, item: GalleryItem, onClick: () -> Unit) {
    val thumb by produceState(ThumbState(null, false), item.key) {
        value = ThumbState(vm.thumbnail(item), true)
    }
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .background(Raised),
                contentAlignment = Alignment.Center
            ) {
                val bitmap = thumb.bitmap
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else if (!thumb.done) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        "No preview",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Column(Modifier.padding(14.dp)) {
                Text(item.category, style = MaterialTheme.typography.titleMedium)
                Text(
                    formatTimestamp(item.time),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    item.status,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (item.inCloud) Success else Warning
                )
            }
        }
    }
}

@Composable
private fun PhotoViewer(
    vm: GalleryViewModel,
    item: GalleryItem,
    onClose: () -> Unit,
    onDelete: (GalleryItem) -> Unit
) {
    val image by produceState(ThumbState(null, false), item.key) {
        value = ThumbState(vm.fullImage(item), true)
    }
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    Column(Modifier.fillMaxSize().padding(20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onClose) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Column(Modifier.weight(1f)) {
                Text(item.category, style = MaterialTheme.typography.titleMedium)
                Text(
                    formatTimestamp(item.time),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = { onDelete(item) }) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = Danger)
            }
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Raised),
            contentAlignment = Alignment.Center
        ) {
            val bitmap = image.bitmap
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                val newScale = (scale * zoom).coerceIn(1f, 5f)
                                scale = newScale
                                offset = if (newScale == 1f) Offset.Zero else offset + pan
                            }
                        }
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                            translationX = offset.x,
                            translationY = offset.y
                        )
                )
            } else if (!image.done) {
                CircularProgressIndicator()
            } else {
                Text(
                    "This photo could not be loaded.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Text(
            if (item.inCloud) {
                "Streamed from Google Drive. Nothing is saved on this phone."
            } else {
                "${item.status}. Stored on this phone until it uploads."
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
