package com.vayora.extension.data

import java.util.Calendar
import kotlinx.coroutines.flow.Flow

class SecurityRepository(private val db: VayoraDatabase) {

    val lastEvent: Flow<SecurityEvent?> = db.eventDao().observeLast()
    val allEvents: Flow<List<SecurityEvent>> = db.eventDao().observeAll()
    val eventCount: Flow<Int> = db.eventDao().observeCount()
    val photoCount: Flow<Int> = db.evidenceDao().observeCount()
    val pendingCount: Flow<Int> = db.evidenceDao().observePendingCount()

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
}
