package com.vayora.extension.data

import java.util.Calendar
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class SecurityRepository(private val db: VayoraDatabase) {

    val lastEvent: Flow<SecurityEvent?> = db.eventDao().observeLast()
    val allEvents: Flow<List<SecurityEvent>> = db.eventDao().observeAll()
    val eventCount: Flow<Int> = db.eventDao().observeCount()
    val photoCount: Flow<Int> = db.evidenceDao().observeCount()
    val unsyncedPhotos: Flow<List<LocalPhotoRow>> = db.evidenceDao().observeUnsynced()
    val pendingCount: Flow<Int> = combine(
        db.evidenceDao().observePendingCount(),
        db.eventDao().observeUnsyncedCount()
    ) { photos, events -> photos + events }

    suspend fun recordEvent(
        type: String,
        details: String = "",
        deviceState: String = ""
    ): Long {
        val now = System.currentTimeMillis()
        val hour = Calendar.getInstance().apply { timeInMillis = now }
            .get(Calendar.HOUR_OF_DAY)
        return db.eventDao().insert(
            SecurityEvent(
                type = type,
                timestamp = now,
                details = details,
                isNight = hour < 6 || hour >= 22,
                deviceState = deviceState
            )
        )
    }

    suspend fun unsyncedEvents(): List<SecurityEvent> = db.eventDao().getUnsynced()

    suspend fun markEventSynced(id: Long) = db.eventDao().markSynced(id)

    suspend fun eventById(id: Long): SecurityEvent? = db.eventDao().getById(id)

    suspend fun pendingPhotos(): List<EvidencePhoto> = db.evidenceDao().getPending()

    suspend fun markPhotoSynced(id: Long, driveId: String) =
        db.evidenceDao().markSynced(id, driveId)

    suspend fun markPhotoFailed(id: Long) = db.evidenceDao().markFailed(id)

    suspend fun addPhoto(photo: EvidencePhoto): Long = db.evidenceDao().insert(photo)

    suspend fun deleteLocalPhoto(id: Long) = db.evidenceDao().deleteById(id)

    suspend fun deletePhotoByDriveId(driveId: String) =
        db.evidenceDao().deleteByDriveId(driveId)
}
