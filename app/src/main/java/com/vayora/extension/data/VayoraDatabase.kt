package com.vayora.extension.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [SecurityEvent::class, EvidencePhoto::class],
    version = 1,
    exportSchema = false
)
abstract class VayoraDatabase : RoomDatabase() {
    abstract fun eventDao(): EventDao
    abstract fun evidenceDao(): EvidenceDao

    companion object {
        fun build(context: Context): VayoraDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                VayoraDatabase::class.java,
                "vayora.db"
            ).build()
    }
}
