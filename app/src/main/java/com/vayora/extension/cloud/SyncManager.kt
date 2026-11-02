package com.vayora.extension.cloud

import android.content.Context
import com.vayora.extension.data.SecurityEvent
import com.vayora.extension.data.SecurityRepository
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

enum class SyncResult { SUCCESS, RETRY, STOPPED }

class SyncManager(
    private val context: Context,
    private val repository: SecurityRepository,
    private val store: DriveStateStore
) {
    private val mutex = Mutex()

    suspend fun runSync(): SyncResult = mutex.withLock { doSync() }

    private suspend fun doSync(): SyncResult = withContext(Dispatchers.IO) {
        val registered = store.state.value.email ?: return@withContext SyncResult.STOPPED
        store.setSyncing(true)
        try {
            val token = try {
                GoogleAuth.silentToken(context, registered)
            } catch (e: Exception) {
                null
            }
            if (token == null) {
                store.setError("Reconnect your Google account")
                return@withContext SyncResult.STOPPED
            }
            val client = DriveClient(token)
            val actual = client.aboutEmail()
            if (!actual.equals(registered, ignoreCase = true)) {
                store.setError("Signed-in Google account does not match the registered account")
                return@withContext SyncResult.STOPPED
            }
            val folderId = client.ensureFolder(store.state.value.folderId)
            store.setFolder(folderId)
            uploadEvents(client, folderId)
            uploadPhotos(client, folderId)
            store.markSynced(System.currentTimeMillis())
            SyncResult.SUCCESS
        } catch (e: DriveException) {
            if (e.code == 401) {
                store.setError("Google session expired. Reconnect your account.")
                SyncResult.STOPPED
            } else {
                store.setError("Drive error ${e.code}. Will retry.")
                SyncResult.RETRY
            }
        } catch (e: IOException) {
            store.setError("No connection. Will retry.")
            SyncResult.RETRY
        } finally {
            store.setSyncing(false)
        }
    }

    private suspend fun uploadEvents(client: DriveClient, folderId: String) {
        val unsynced = repository.unsyncedEvents()
        for (batch in unsynced.chunked(500)) {
            val array = JSONArray()
            for (event in batch) {
                array.put(eventJson(event))
            }
            val root = JSONObject()
                .put("count", batch.size)
                .put("events", array)
            client.upload(
                name = "events_${batch.first().timestamp}_${batch.last().timestamp}.json",
                mime = "application/json",
                bytes = root.toString(2).toByteArray(Charsets.UTF_8),
                parentId = folderId,
                props = mapOf(
                    "kind" to "eventlog",
                    "count" to batch.size.toString(),
                    "capturedAt" to batch.first().timestamp.toString(),
                    "category" to "Event log"
                )
            )
            for (event in batch) {
                repository.markEventSynced(event.id)
            }
        }
    }

    private fun eventJson(event: SecurityEvent): JSONObject =
        JSONObject()
            .put("id", event.id)
            .put("type", event.type)
            .put("timestamp", event.timestamp)
            .put("time", isoTime(event.timestamp))
            .put("details", event.details)
            .put("isNight", event.isNight)
            .put("deviceState", event.deviceState)

    private suspend fun uploadPhotos(client: DriveClient, folderId: String) {
        for (photo in repository.pendingPhotos()) {
            val path = photo.localPath
            val file = if (path != null) File(path) else null
            if (file == null || !file.exists()) {
                repository.markPhotoFailed(photo.id)
                continue
            }
            val event = repository.eventById(photo.eventId)
            val driveId = client.upload(
                name = "evidence_${photo.eventId}_${photo.capturedAt}.jpg",
                mime = "image/jpeg",
                bytes = file.readBytes(),
                parentId = folderId,
                props = mapOf(
                    "kind" to "photo",
                    "eventId" to photo.eventId.toString(),
                    "capturedAt" to photo.capturedAt.toString(),
                    "category" to (event?.type ?: "Unknown")
                )
            )
            repository.markPhotoSynced(photo.id, driveId)
            file.delete()
        }
    }

    private fun isoTime(ms: Long): String =
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US).format(Date(ms))
}
