package com.vayora.extension.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

data class LocalPhotoRow(
    val id: Long,
    val eventId: Long,
    val localPath: String?,
    val capturedAt: Long,
    val syncState: String,
    val category: String?
)

@Dao
interface EventDao {
    @Insert
    suspend fun insert(event: SecurityEvent): Long

    @Query("SELECT * FROM security_events ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<SecurityEvent>>

    @Query("SELECT * FROM security_events ORDER BY timestamp DESC LIMIT 1")
    fun observeLast(): Flow<SecurityEvent?>

    @Query("SELECT COUNT(*) FROM security_events")
    fun observeCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM security_events WHERE synced = 0")
    fun observeUnsyncedCount(): Flow<Int>

    @Query("SELECT * FROM security_events WHERE synced = 0 ORDER BY timestamp ASC")
    suspend fun getUnsynced(): List<SecurityEvent>

    @Query("UPDATE security_events SET synced = 1 WHERE id = :id")
    suspend fun markSynced(id: Long)

    @Query("SELECT * FROM security_events WHERE id = :id")
    suspend fun getById(id: Long): SecurityEvent?
}

@Dao
interface EvidenceDao {
    @Insert
    suspend fun insert(photo: EvidencePhoto): Long

    @Query("SELECT COUNT(*) FROM evidence_photos")
    fun observeCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM evidence_photos WHERE syncState = 'PENDING'")
    fun observePendingCount(): Flow<Int>

    @Query("SELECT * FROM evidence_photos WHERE syncState = 'PENDING' ORDER BY capturedAt ASC")
    suspend fun getPending(): List<EvidencePhoto>

    @Query(
        "SELECT p.id AS id, p.eventId AS eventId, p.localPath AS localPath, " +
            "p.capturedAt AS capturedAt, p.syncState AS syncState, e.type AS category " +
            "FROM evidence_photos p LEFT JOIN security_events e ON e.id = p.eventId " +
            "WHERE p.syncState != 'SYNCED' ORDER BY p.capturedAt DESC"
    )
    fun observeUnsynced(): Flow<List<LocalPhotoRow>>

    @Query(
        "UPDATE evidence_photos SET syncState = 'SYNCED', driveFileId = :driveId, " +
            "localPath = NULL WHERE id = :id"
    )
    suspend fun markSynced(id: Long, driveId: String)

    @Query("UPDATE evidence_photos SET syncState = 'FAILED' WHERE id = :id")
    suspend fun markFailed(id: Long)

    @Query("DELETE FROM evidence_photos WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM evidence_photos WHERE driveFileId = :driveId")
    suspend fun deleteByDriveId(driveId: String)
}
