package com.vayora.extension.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "security_events")
data class SecurityEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,
    val timestamp: Long,
    val details: String = "",
    val isNight: Boolean = false,
    val deviceState: String = "",
    val synced: Boolean = false
)

@Entity(tableName = "evidence_photos", indices = [Index("eventId")])
data class EvidencePhoto(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val eventId: Long,
    val localPath: String? = null,
    val driveFileId: String? = null,
    val capturedAt: Long,
    val syncState: String = "PENDING"
)
