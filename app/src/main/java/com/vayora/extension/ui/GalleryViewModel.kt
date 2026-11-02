package com.vayora.extension.ui

import android.graphics.Bitmap
import android.util.LruCache
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.vayora.extension.VayoraApp
import com.vayora.extension.cloud.CloudPhoto
import com.vayora.extension.cloud.DriveClient
import com.vayora.extension.cloud.DriveException
import com.vayora.extension.cloud.GoogleAuth
import com.vayora.extension.cloud.ImageUtil
import java.io.File
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext

data class GalleryItem(
    val key: String,
    val cloudId: String?,
    val localId: Long?,
    val localPath: String?,
    val category: String,
    val time: Long,
    val thumbnailLink: String?,
    val inCloud: Boolean,
    val status: String
)

data class CloudListState(
    val loading: Boolean = false,
    val photos: List<CloudPhoto> = emptyList(),
    val error: String? = null,
    val loadedOnce: Boolean = false
)

class GalleryViewModel(private val app: VayoraApp) : ViewModel() {

    private val store = app.driveState
    private val repository = app.repository

    private val _cloud = MutableStateFlow(CloudListState())
    val cloudState: StateFlow<CloudListState> = _cloud.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    val items: StateFlow<List<GalleryItem>> = combine(
        _cloud,
        repository.unsyncedPhotos
    ) { cloud, local ->
        val pending = local.map { row ->
            GalleryItem(
                key = "local-${row.id}",
                cloudId = null,
                localId = row.id,
                localPath = row.localPath,
                category = row.category ?: "Unknown",
                time = row.capturedAt,
                thumbnailLink = null,
                inCloud = false,
                status = if (row.syncState == "FAILED") "Upload failed" else "Pending upload"
            )
        }
        val remote = cloud.photos.map { photo ->
            GalleryItem(
                key = "cloud-${photo.id}",
                cloudId = photo.id,
                localId = null,
                localPath = null,
                category = photo.category,
                time = photo.time,
                thumbnailLink = photo.thumbnailLink,
                inCloud = true,
                status = "In Google Drive"
            )
        }
        (pending + remote).sortedByDescending { it.time }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val thumbCache = object : LruCache<String, Bitmap>(24 * 1024) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount / 1024
    }
    private val fullCache = object : LruCache<String, Bitmap>(48 * 1024) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount / 1024
    }

    private val tokenMutex = Mutex()
    private val downloadGate = Semaphore(3)
    private var cachedToken: String? = null
    private var tokenTime = 0L

    private suspend fun client(): DriveClient = tokenMutex.withLock {
        val now = System.currentTimeMillis()
        val cached = cachedToken
        if (cached != null && now - tokenTime < TOKEN_TTL_MS) {
            return@withLock DriveClient(cached)
        }
        val email = store.state.value.email
            ?: throw IllegalStateException("Connect your Google account first.")
        val token = GoogleAuth.silentToken(app, email)
            ?: throw IllegalStateException("Reconnect your Google account.")
        cachedToken = token
        tokenTime = now
        DriveClient(token)
    }

    private fun noteError(e: Exception) {
        if (e is DriveException && e.code == 401) {
            cachedToken = null
        }
    }

    fun clearMessage() {
        _message.value = null
    }

    fun refresh() {
        viewModelScope.launch {
            _cloud.update { it.copy(loading = true, error = null) }
            try {
                val folderId = store.state.value.folderId
                    ?: throw IllegalStateException(
                        "No evidence folder yet. Connect Google and sync first."
                    )
                val list = withContext(Dispatchers.IO) { client().listPhotos(folderId) }
                _cloud.value = CloudListState(false, list, null, true)
            } catch (e: DriveException) {
                noteError(e)
                _cloud.update {
                    it.copy(loading = false, error = "Google Drive error: ${e.message}")
                }
            } catch (e: IllegalStateException) {
                _cloud.update { it.copy(loading = false, error = e.message) }
            } catch (e: IOException) {
                _cloud.update { it.copy(loading = false, error = "No connection to Google.") }
            } catch (e: Exception) {
                _cloud.update {
                    it.copy(loading = false, error = "Could not load the gallery: ${e.message}")
                }
            }
        }
    }

    suspend fun thumbnail(item: GalleryItem): Bitmap? = withContext(Dispatchers.IO) {
        val cached = thumbCache.get(item.key)
        if (cached != null) return@withContext cached
        val bitmap = try {
            downloadGate.withPermit {
                val path = item.localPath
                if (path != null) {
                    ImageUtil.decodeFile(path, THUMB_SIZE)
                } else {
                    loadCloudThumbnail(item)
                }
            }
        } catch (e: Exception) {
            noteError(e)
            null
        }
        if (bitmap != null) thumbCache.put(item.key, bitmap)
        bitmap
    }

    private suspend fun loadCloudThumbnail(item: GalleryItem): Bitmap? {
        val id = item.cloudId ?: return null
        val client = client()
        val link = item.thumbnailLink
        if (link != null) {
            try {
                val sized = link.replace(Regex("=s\\d+$"), "=s$THUMB_SIZE")
                val bytes = client.fetchBytes(sized)
                val decoded = ImageUtil.decodeBytes(bytes, THUMB_SIZE)
                if (decoded != null) return decoded
            } catch (e: IOException) {
                // fall back to downloading the image itself
            }
        }
        return ImageUtil.decodeBytes(client.downloadMedia(id), THUMB_SIZE)
    }

    suspend fun fullImage(item: GalleryItem): Bitmap? = withContext(Dispatchers.IO) {
        val cached = fullCache.get(item.key)
        if (cached != null) return@withContext cached
        val bitmap = try {
            val path = item.localPath
            val id = item.cloudId
            if (path != null) {
                ImageUtil.decodeFile(path, FULL_SIZE)
            } else if (id != null) {
                ImageUtil.decodeBytes(client().downloadMedia(id), FULL_SIZE)
            } else {
                null
            }
        } catch (e: Exception) {
            noteError(e)
            null
        }
        if (bitmap != null) fullCache.put(item.key, bitmap)
        bitmap
    }

    fun delete(item: GalleryItem) {
        viewModelScope.launch {
            try {
                val cloudId = item.cloudId
                val localId = item.localId
                if (cloudId != null) {
                    withContext(Dispatchers.IO) { client().deleteFile(cloudId) }
                    repository.deletePhotoByDriveId(cloudId)
                    _cloud.update { state ->
                        state.copy(photos = state.photos.filterNot { it.id == cloudId })
                    }
                } else if (localId != null) {
                    repository.deleteLocalPhoto(localId)
                    item.localPath?.let { File(it).delete() }
                }
                thumbCache.remove(item.key)
                fullCache.remove(item.key)
                _message.value = "Evidence deleted."
            } catch (e: Exception) {
                noteError(e)
                _message.value = "Could not delete: ${e.message}"
            }
        }
    }

    /** Drops every in-memory image and the cached token. Called whenever the app locks. */
    fun clear() {
        thumbCache.evictAll()
        fullCache.evictAll()
        cachedToken = null
        _cloud.value = CloudListState()
    }

    companion object {
        private const val THUMB_SIZE = 400
        private const val FULL_SIZE = 2048
        private const val TOKEN_TTL_MS = 20L * 60L * 1000L

        fun factory(app: VayoraApp): ViewModelProvider.Factory =
            viewModelFactory {
                initializer { GalleryViewModel(app) }
            }
    }
}
