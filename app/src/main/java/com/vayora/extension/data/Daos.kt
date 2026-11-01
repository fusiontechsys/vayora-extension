package com.vayora.extension.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

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
}

@Dao
interface EvidenceDao {
    @Insert
    suspend fun insert(photo: EvidencePhoto): Long

    @Query("SELECT COUNT(*) FROM evidence_photos")
    fun observeCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM evidence_photos WHERE syncState != 'SYNCED'")
    fun observePendingCount(): Flow<Int>
}
